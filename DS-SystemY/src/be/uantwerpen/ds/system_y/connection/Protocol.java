package be.uantwerpen.ds.system_y.connection;

/**
 * The protocol used for commands between nodes/server
 */
public enum Protocol {
	DISCOVER,
	DISCOVER_ACK,
	NODE_JOINED,
	SET_NODES,
	SET_PREVNODE,
	SET_NEXTNODE,
	PING,
	PING_ACK,
	FILE_LOCATION_AVAILABLE,
	FILE_LOCATION_UNAVAILABLE,
	DOWNLOAD_REQUEST,
}
