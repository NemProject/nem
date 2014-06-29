private static class NodeRefresher {
	final Node localNode;
	final PreTrustedNodes preTrustedNodes;
	final NodeCollection nodes;
	final PeerConnector connector;
	final Map<Node, NodeStatus> nodesToUpdate;
	final ConcurrentHashSet<Node> connectedNodes;

	public NodeRefresher(
			final Node localNode,
			final PreTrustedNodes preTrustedNodes,
			final NodeCollection nodes,
			final PeerConnector connector) {
		this.localNode = localNode;
		this.preTrustedNodes = preTrustedNodes;
		this.nodes = nodes;
		this.connector = connector;
		this.nodesToUpdate = new ConcurrentHashMap<>();
		this.connectedNodes = new ConcurrentHashSet<>();
	}

	public CompletableFuture<Void> refresh() {
		// all refresh nodes are directly communicated with;
		// ensure that only direct communication is trusted for these nodes
		final Set<Node> refreshNodes = this.getRefreshNodes();
		this.connectedNodes.addAll(refreshNodes);

		final List<CompletableFuture> futures = refreshNodes.stream()
				.map(n -> this.getNodeInfo(n, true))
				.collect(Collectors.toList());

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]))
				.whenComplete((o, e) -> {
					for (final Map.Entry<Node, NodeStatus> entry : this.nodesToUpdate.entrySet())
						this.nodes.update(entry.getKey(), entry.getValue());
				});
	}

	private Set<Node> getRefreshNodes() {
		// always include pre-trusted nodes even if they previously resulted in a failure
		final Set<Node> refreshNodes = new HashSet<>(this.nodes.getAllNodes());
		refreshNodes.addAll(this.preTrustedNodes.getNodes().stream().collect(Collectors.toList()));
		return refreshNodes;
	}

	private CompletableFuture<Void> getNodeInfo(final Node node, boolean isDirectContact) {
		// never sync with the local node or an indirect node that has already been communicated with
		if (this.localNode.equals(node) || (!isDirectContact && !this.connectedNodes.add(node))) {
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<NodeStatus> future = this.connector.getInfo(node)
				.thenApply(n -> {
					// if the node returned inconsistent information, drop it for this round
					if (!areCompatible(node, n))
						throw new FatalPeerException("node response is not compatible with node identity");

					node.setEndpoint(n.getEndpoint());
					node.setMetaData(n.getMetaData());
					return NodeStatus.ACTIVE;
				});

		if (isDirectContact) {
			future = future
					.thenCompose(v -> this.connector.getKnownPeers(node))
					.thenCompose(nodes -> {
						final List<CompletableFuture> futures = nodes.asCollection().stream()
								.map(n -> this.getNodeInfo(n, false))
								.collect(Collectors.toList());

						return CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
					})
					.thenApply(v -> NodeStatus.ACTIVE);
		}

		return future
				.exceptionally(obj -> this.getNodeStatusFromException(obj))
				.thenAccept(ns -> this.update(node, ns));
	}

	private NodeStatus getNodeStatusFromException(Throwable ex) {
		ex = CompletionException.class == ex.getClass() ? ex.getCause() : ex;
		return InactivePeerException.class == ex.getClass() ? NodeStatus.INACTIVE : NodeStatus.FAILURE;
	}

	private static boolean areCompatible(final Node lhs, final Node rhs) {
		return lhs.equals(rhs);
	}

	private void update(final Node node, final NodeStatus status) {
		if (status == this.nodes.getNodeStatus(node) || this.localNode.equals(node))
			return;

		LOGGER.info(String.format("Updating \"%s\" -> %s", node, status));
		this.nodesToUpdate.put(node, status);
	}
}