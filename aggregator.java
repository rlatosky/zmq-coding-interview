import org.apache.commons.cli.*;
import org.zeromq.*;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class aggregator {
	private static final AtomicBoolean running = new AtomicBoolean(true);

	public static void main(String[] args) {

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			running.set(false);
		}));

		Options options = new Options();

		Option pubport = new Option("p", "port", true, "TCP port number to listen on for connections.");
		pubport.setRequired(true);
		options.addOption(pubport);
		Option sp1 = new Option("sp1", "subport1", true, "Producer1 TCP port number to connect to.");
		sp1.setRequired(true);
		options.addOption(sp1);
		Option sp2 = new Option("sp2", "subport2", true, "Producer2 TCP port number to connect to.");
		options.addOption(sp2);
		Option sp3 = new Option("sp3", "subport3", true, "Producer3 TCP port number to connect to.");
		options.addOption(sp3);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;

		try {
			cmd = parser.parse(options, args);
			String port = cmd.getOptionValue("port");
			String subport1 = cmd.getOptionValue("subport1");
			String subport2 = cmd.getOptionValue("subport2");
			String subport3 = cmd.getOptionValue("subport3");

			try (ZContext context = new ZContext()) {

				ZMQ.Socket sub1 = context.createSocket(ZMQ.PULL);
				sub1.bind("tcp://localhost:" + subport1);
				ZMQ.Socket sub2 = context.createSocket(ZMQ.PULL);
				sub2.bind("tcp://localhost:" + subport2);
				ZMQ.Socket sub3 = context.createSocket(ZMQ.PULL);
				sub3.bind("tcp://localhost:" + subport3);

				List<Byte> combinedBuffer = new ArrayList<>();

				Poller poller = context.createPoller(3);
				poller.register(sub1, Poller.POLLIN);
				poller.register(sub2, Poller.POLLIN);
				poller.register(sub3, Poller.POLLIN);

				// Publish to clients
//				ZMQ.Socket pub = context.createSocket(ZMQ.REP);
//				pub.bind("tcp://*:" + port);

				int receivedBuffers = 0;

				while (running.get()) {
					// Poll the sockets to see if there's data to be received
					poller.poll();

					for (int i = 0; i < poller.getSize(); i++) {
						if (poller.pollin(i)) {
							ZMQ.Socket socket = poller.getSocket(i);
							byte[] message = socket.recv(0);

							// Add the received data to the combined buffer
							for (byte b : message) {
								combinedBuffer.add(b);
							}

							System.out.println("Received a message of size: " + message.length);
							//receivedBuffers++;
						}
					}
				}
				// Convert List<Byte> to byte[]
				byte[] finalBuffer = new byte[combinedBuffer.size()];
				for (int i = 0; i < combinedBuffer.size(); i++) {
					finalBuffer[i] = combinedBuffer.get(i);
				}

				// Create a PUSH socket to send the combined buffer
				ZMQ.Socket pushSocket = context.createSocket(ZMQ.PUSH);
				pushSocket.connect("tcp://localhost:" + port);

				// Send the combined buffer
				if (pushSocket.send(finalBuffer, 0)) {
					System.out.println("Combined buffer sent successfully!");
				} else {
					System.out.println("Failed to send the combined buffer.");
				}
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(1);
		}


	}
}
