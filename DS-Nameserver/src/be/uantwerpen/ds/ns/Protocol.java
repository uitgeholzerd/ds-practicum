package be.uantwerpen.ds.ns;

/**
 * The protocol used for commands between nodes/server
 */
public enum Protocol {
	DISCOVER,
	DISCOVER_ACK,
	SET_NODES,
	SET_PREVNODE,
	SET_NEXTNODE,
	FAIL,
	PING,
	PING_ACK
}
