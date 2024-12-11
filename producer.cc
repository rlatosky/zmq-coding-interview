#include <zmq.hpp>
#include <argparse/argparse.hpp>

#include <string>
#include <iostream>

int main(int argc, char *argv[]) {

	argparse::ArgumentParser program("producer");
	program.add_argument("-p", "port")
		.help("TCP port number to listen on for connections.")
		.required();

	program.add_argument("-s","--size")
		.help("Size of data buffers to send to receiving end.")
		.default_value(float(8));

	program.add_argument("-dr","--datarate")
		.help("Rate in kB/s that data should be sent.")
		.default_value(float(.01))
		.required();

	try {
		program.parse_args(argc, argv);
	} catch (const std::runtime_error& err) {
		std::cerr << err.what() << std::endl;
		std::cerr << program;
		std::exit(1);
	}

	// Get argument and convert to string
	auto port = program.get<std::string>("-p");
	auto size = program.get<float>("-s");
	auto dr = program.get<float>("-dr");

	srand(static_cast<unsigned>(time(0)));

	// Create a new context with a single IO thread
	zmq::context_t context(1);

	sleep(3);
	try {
		std::vector<uint8_t> buffer(size);
		while (true) {
			for (int i = 0; i < size; i++) {
				if (i < dr*1000)
			 		buffer[i] = rand() % 256;
			 		// Since we want only 8-bit numbers, if we modulo 256 from a random
		 		else
		 			break;
					// number, we receive any random 8-bit number from 0-255.
			}

			// Create a socket of type PUSH
			zmq::socket_t socket(context, ZMQ_PUSH);
			socket.connect("tcp://localhost:" + port);

			// The data you want to send


			zmq::message_t message(buffer.size());

			// Copy the data into the message
			//memcpy(message.data(), buffer.data(), buffer.size());

			if (message.data() != nullptr) {
				memcpy(message.data(), buffer.data(), buffer.size());
			} else {
				std::cerr << "Message data is null..." << std::endl;
			}

			// Send the message
			socket.send(message, zmq::send_flags::none);

			//std::cout << "Message sent!";
			//std::cout << message << std::endl;
			buffer.clear();
			sleep(1);
		}
	} catch (const std::exception& e) {
		std::cerr << "Exception Caught: " << e.what() << std::endl;
		return 0;
	}
}
