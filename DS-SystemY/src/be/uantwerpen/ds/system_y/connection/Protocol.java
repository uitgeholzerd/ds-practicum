package be.uantwerpen.ds.system_y.connection;

/**
 * The protocol used for commands between nodes/server
 */
public enum Protocol {
	DISCOVER,						// Client is searching for the name server in the network
	DISCOVER_ACK,					// Name server acknowledges clients DISCOVER
	NODE_JOINED,					// New node has joing the network
	SET_NODES,						// Set neighbours to specified nodes 
	SET_PREVNODE,					// Set previous neighbour to specified node
	SET_NEXTNODE,					// Set next neighbour to specified node
	PING,							// Ping node
	PING_ACK,						// Acknowlegde ping
	FILE_LOCATION_AVAILABLE,		// Report file location available
	FILE_LOCATION_UNAVAILABLE,		// Report file location unavailable
	SEND_FILE,						// Incoming file transmision
	CHECK_OWNER,					// Check if node owns file
	DOWNLOAD_REQUEST,				// Request file downlaod
	DELETE_LOCAL_REQUEST,			// Request deletion of downloadlocation
	DELETE_NETWORK_REQUEST,			// Request deletion of file all over the network
	FILE_LOCATIONS_REQUEST,			// Ask the owner for the locations of the file
	FILE_LOCATIONS_ACK
}
