//JAVA 21+
//DEPS org.symbol:symbol-sdk:3.3.3

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class GetNamespaceInfo {
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final HttpClient HTTP_CLIENT =
		HttpClient.newHttpClient();

	private static final String NODE_URL = System.getenv().getOrDefault(
		"NODE_URL", "http://libertalia.nemtest.net:7890");

	public static void main(final String[] args) {
		try {
			new GetNamespaceInfo().run();
		} catch (final Exception ex) {
			System.out.println(null == ex.getMessage()
				? ex.toString()
				: ex.getMessage());
		}
	}

	private void run() throws IOException, InterruptedException {
		System.out.printf("Using node %s%n", NODE_URL);

		final String namespaceName = System.getenv().getOrDefault(
			"NAMESPACE_NAME", "company");
		System.out.printf("Namespace name: %s%n", namespaceName);

		// Fetch namespace information [>step-1]
		final String namespacePath = String.format(
			"/namespace?namespace=%s", namespaceName);
		System.out.printf("Fetching namespace information from %s%n",
			namespacePath);
		final HttpRequest namespaceRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + namespacePath)).GET().build();
		final HttpResponse<String> namespaceResponse = HTTP_CLIENT.send(
			namespaceRequest, BodyHandlers.ofString());
		if (2 != namespaceResponse.statusCode() / 100)
			throw new IOException("HTTP error! status: "
				+ namespaceResponse.statusCode());
		final JsonNode namespaceInfo = JSON_MAPPER.readTree(
			namespaceResponse.body());
		System.out.println("Namespace information:");
		System.out.printf("  Name: %s%n",
			namespaceInfo.get("fqn").asText());
		System.out.printf("  Owner: %s%n",
			namespaceInfo.get("owner").asText());
		final long leaseHeight = namespaceInfo.get("height").asLong();
		System.out.printf("  Height: %d%n", leaseHeight);
		// [<step-1]
		// Compute the lease expiration [>step-2]
		final long leaseDuration = 525_600;
		final HttpRequest heightRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + "/chain/height")).GET().build();
		final HttpResponse<String> heightResponse = HTTP_CLIENT.send(
			heightRequest, BodyHandlers.ofString());
		final long currentHeight = JSON_MAPPER.readTree(
			heightResponse.body()).get("height").asLong();
		final long expirationHeight = leaseHeight + leaseDuration;
		System.out.printf("%nCurrent chain height: %d%n", currentHeight);
		System.out.printf("Lease expiration height: %d%n",
			expirationHeight);
		System.out.printf("Blocks until expiration: %d%n",
			expirationHeight - currentHeight);
		// [<step-2]
		// List the subnamespaces [>step-3]
		final String owner = namespaceInfo.get("owner").asText();
		final String subnamespacesPath = String.format(
			"/account/namespace/page?address=%s&parent=%s",
			owner, namespaceName);
		System.out.printf("%nFetching subnamespaces from %s%n",
			subnamespacesPath);
		final HttpRequest subnamespacesRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + subnamespacesPath)).GET().build();
		final HttpResponse<String> subnamespacesResponse =
			HTTP_CLIENT.send(
				subnamespacesRequest, BodyHandlers.ofString());
		final JsonNode subnamespaces = JSON_MAPPER.readTree(
			subnamespacesResponse.body()).get("data");
		System.out.printf("Subnamespaces of %s: %d%n",
			namespaceName, subnamespaces.size());
		for (final JsonNode subnamespace : subnamespaces)
			System.out.printf("  %s%n",
				subnamespace.get("fqn").asText());
		// [<step-3]
		// List the mosaics defined under the namespace [>step-4]
		final String mosaicsPath = String.format(
			"/namespace/mosaic/definition/page?namespace=%s",
			namespaceName);
		System.out.printf("%nFetching mosaic definitions from %s%n",
			mosaicsPath);
		final HttpRequest mosaicsRequest = HttpRequest.newBuilder(
			URI.create(NODE_URL + mosaicsPath)).GET().build();
		final HttpResponse<String> mosaicsResponse = HTTP_CLIENT.send(
			mosaicsRequest, BodyHandlers.ofString());
		final JsonNode mosaics = JSON_MAPPER.readTree(
			mosaicsResponse.body()).get("data");
		System.out.printf("Mosaics defined under %s: %d%n",
			namespaceName, mosaics.size());
		for (final JsonNode entry : mosaics) {
			final JsonNode id = entry.get("mosaic").get("id");
			System.out.printf("  %s:%s%n",
				id.get("namespaceId").asText(),
				id.get("name").asText());
		}
		// [<step-4]
	}
}
