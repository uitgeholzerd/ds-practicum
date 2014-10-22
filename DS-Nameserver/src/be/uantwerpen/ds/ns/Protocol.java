package be.uantwerpen.ds.ns;

/**
 * The protocol used for commands between nodes/server
 */
public enum Protocol {
	DISCOVER,
	DISCOVER_ACK,
	SET_NODES,
	LEAVE,
	ACTION_FAILED,
	FAIL,
	PREVNODE,
	NEXTNODE,
	PING,
	PING_ACK
}
