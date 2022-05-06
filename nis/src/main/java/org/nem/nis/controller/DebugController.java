package org.nem.nis.controller;

import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.serialization.SerializableList;
import org.nem.core.time.TimeSynchronizationResult;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.controller.annotations.PublicApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Controller that exposes debug endpoints.
 */
@RestController
public class DebugController {
	private final NisPeerNetworkHost host;

	/**
	 * Creates a new debug controller.
	 *
	 * @param host The host.
	 */
	@Autowired(required = true)
	public DebugController(final NisPeerNetworkHost host) {
		this.host = host;
	}

	/**
	 * Gets debug information about all running timers.
	 *
	 * @return Debug information about all running timers.
	 */
	@RequestMapping(value = "/debug/timers", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<NemAsyncTimerVisitor> timersInfo() {
		return new SerializableList<>(this.host.getVisitors());
	}

	/**
	 * Gets debug information about incoming connections.
	 *
	 * @return Debug information about incoming connections.
	 */
	@RequestMapping(value = "/debug/connections/incoming", method = RequestMethod.GET)
	@PublicApi
	public AuditCollection incomingConnectionsInfo() {
		return this.host.getIncomingAudits();
	}

	/**
	 * Gets debug information about outgoing connections.
	 *
	 * @return Debug information about outgoing connections.
	 */
	@RequestMapping(value = "/debug/connections/outgoing", method = RequestMethod.GET)
	@PublicApi
	public AuditCollection outgoingConnectionsInfo() {
		return this.host.getOutgoingAudits();
	}

	/**
	 * Gets debug information about the time synchronization.
	 *
	 * @return Debug information about the time synchronization.
	 */
	@RequestMapping(value = "/debug/time-synchronization", method = RequestMethod.GET)
	@PublicApi
	public SerializableList<TimeSynchronizationResult> timeSynchronizationInfo() {
		return new SerializableList<>(this.host.getNetwork().getTimeSynchronizationResults());
	}
}
