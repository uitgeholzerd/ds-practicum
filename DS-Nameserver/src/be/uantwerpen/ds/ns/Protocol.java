package be.uantwerpen.ds.ns;

/**
 * The protocol used for commands between nodes/server
 *
 */
public enum Protocol {
	DISCOVER,
	DISCOVER_ACK,
	PREVNODE,
	NEXTNODE,
	LEAVE,
	ACTION_FAILED
}
