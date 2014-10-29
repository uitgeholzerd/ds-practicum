package be.uantwerpen.ds.system_y;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;

import be.uantwerpen.ds.system_y.client.Client;

public class FileWatcher implements Runnable {
	private Client client;
	private WatchService watcher;
	private Path dir;

	// TODO client meegeven?
	public FileWatcher(Client client, String path) {
		this.client = client;
		dir = Paths.get(path);
		try {
			watcher = FileSystems.getDefault().newWatchService();
			dir.register(watcher, ENTRY_CREATE);
		} catch (IOException e) {
			System.err.println("Error while initializing WatchService");
			e.printStackTrace();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	// This method uses the WatchService to check for changes in a certain folder
	// When a new file is created, an event is triggered and the client is informed
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			System.out.println("*** Watching folder: " + dir);
			WatchKey key;
			try {
				//Thread will freeze here untill the watchservice is triggered
				key = watcher.take();
				System.out.println("*** WatchService triggered");
			} catch (InterruptedException e) {
				System.err.println("Interrupted while waiting for WatchKey");
				Thread.currentThread().interrupt();
				e.printStackTrace();
				break;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				// Overflow occures when there are too much of WatchEvents in rapid succession
				if (kind == OVERFLOW) {
					System.err.println("Overflow WatchEvent triggered");
					continue;
				}

				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filepath = dir.resolve(ev.context());
				
				// If the WatchEvent was triggered by a CREATE, inform the client.
				if (kind == ENTRY_CREATE) {
					Date now = new Date();
					System.out.println(kind.toString() + ": " + filepath + " - " + now.toString());
					if (client != null) {
						client.newFilesFound();
					}
				}

			}
			// Reset the WatchKey and check if  it's still valid
			if (!key.reset()) {
				System.err.println("WatchKey is not valid");
				break;
			}
		}
		try {
			watcher.close();
		} catch (IOException e) {
			System.err.println("Error while closing the watcher");
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		FileWatcher watcher = new FileWatcher(null, "C:\\");
		Thread t = new Thread(watcher);
		t.start();
	}
	
}


