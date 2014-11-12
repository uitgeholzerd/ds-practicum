package be.uantwerpen.ds.system_y.connection;

/**
 * The protocol used for commands between nodes/server
 */
public enum Protocol {
	DISCOVER,
	DISCOVER_ACK,
	SET_NODES,
	SET_PREVNODE,
	SET_NEXTNODE,
	PING,
	PING_ACK,
	UPDATE_FILERECORD,
	DOWNLOAD_REQUEST
}
