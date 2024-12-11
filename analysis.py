import argparse;
import zmq; # From `pip install pyzmq`

parser = argparse.ArgumentParser(description='Analysis program from Java Aggregator')
parser.add_argument("-sp", "--subport", help="Aggregator TCP port number to connect to.")
parser.add_argument("-t", "--time", help="Wall clock time in seconds to run before quitting.")

args = parser.parse_args()

# Create a ZeroMQ context
context = zmq.Context()

# Create a subscriber socket
subscriber = context.socket(zmq.SUB)
subscriber.connect("tcp://localhost:" + args.subport)

# Subscribe to a specific topic
topic_filter = "aggregator"
subscriber.setsockopt_string(zmq.SUBSCRIBE, topic_filter)

print(f"Listening for messages on topic: {topic_filter}")

while True:
	# Receive the topic and message
	topic = subscriber.recv_string()
	message = subscriber.recv_string()
