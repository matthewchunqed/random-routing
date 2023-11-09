// =============================================================================
// IMPORTS

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
// =============================================================================



// =============================================================================
/**
 * @file   RandomNetworkLayer.java
 * @author Scott F. Kaplan (sfkaplan@cs.amherst.edu)
 * @date   April 2022
 *
 * A network layer that perform routing via random link selection.
 */
public class RandomNetworkLayer extends NetworkLayer {
// =============================================================================



    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================



    // =========================================================================
    /**
     * Default constructor.  Set up the random number generator.
     */
    public RandomNetworkLayer () {

	random = new Random();

    } // RandomNetworkLayer ()
    // =========================================================================

    

    // =========================================================================
    /**
     * Create a single packet containing the given data, with header that marks
     * the source and destination hosts.
     *
     * @param destination The address to which this packet is sent.
     * @param data        The data to send.
     * @return the sequence of bytes that comprises the packet.
     */
    protected byte[] createPacket (int destination, byte[] data) {

    // this method must add the metadata: packet number, destination, source
	
    List<Byte> packet = new ArrayList<Byte>();
    //this will hold the data until the final recast to a byte[].

    //first, add the length of the data
    packet.add((byte)(data.length)); 

    //adds the int destination into the packet in the form of four bytes.
    packet.add((byte)(0xff & (destination >> 24)));
    packet.add((byte)(0xff & (destination >> 16)));
    packet.add((byte)(0xff & (destination >> 8)));
    packet.add((byte)(0xff & destination));
    //with this convention, 6000 = {6, 0, 0, 0}.

    //next, add the source into the packet in the form of four bytes.
    packet.add((byte)(0xff & (address >> 24)));
    packet.add((byte)(0xff & (address >> 16)));
    packet.add((byte)(0xff & (address >> 8)));
    packet.add((byte)(0xff & address)); 

    //then add the rest of the (actual) data to the packet
    for(int i=0; i<data.length; i++){
        packet.add(data[i]);
    }
    
    //and return the result as a byte[]
    int packetSize = packet.size();
    byte[] returnedPacket = new byte[packetSize];
    for(int i=0; i<packetSize; i++){
        returnedPacket[i] = packet.get(i);
    }

    return returnedPacket;
	
    } // createPacket ()
    // =========================================================================



    // =========================================================================
    /**
     * Randomly choose the link through which to send a packet given its
     * destination.
     *
     * @param destination The address to which this packet is being sent.
     */
    protected DataLinkLayer route (int destination) {

    //The pool of randomly selectable link layers, indexed by keys
    List<Integer> randomLinkLayers = new ArrayList<Integer>();

    //check if an adjacent datalinklayer is the destination one
    for(int key : dataLinkLayers.keySet()){
        randomLinkLayers.add(key);
        if(key == destination){
            return dataLinkLayers.get(key);
        }
    }

    //randomly chooses a key inside randomLinkLayers
    int randomKey = random.nextInt(randomLinkLayers.size());
    return dataLinkLayers.get(randomLinkLayers.get(randomKey));

    } // route ()
    // =========================================================================



    // =========================================================================
    /**
     * Examine a buffer to see if its data can be extracted as a packet; if so,
     * do it, and return the packet whole.
     *
     * @param buffer The receive-buffer to be examined.
     * @return the packet extracted packet if a whole one is present in the
     *         buffer; <code>null</code> otherwise.
     */
    protected byte[] extractPacket (Queue<Byte> buffer) {
    
    //edge case for no data received yet.
    if(buffer.size() < 8){
        return null;
    }

    //pulls the length from the metadata
    int dataLength = (int)(buffer.peek());

    int bufferSize = buffer.size();
    //if it's a full packet, remove the data from the buffer and return it as a byte[].
	if(bufferSize >= (9 + dataLength)){

        //empty buffer and place into byte array
        byte[] target = new byte[bufferSize];
        //the loop must start at 1 to skip the length metadata
        for(int i=0; i < bufferSize; i++){
            target[i] = buffer.remove();
        }
        return target;
    }

    //if it's not a full packet, return null
    return null;
    
    } // extractPacket ()
    // =========================================================================



    // =========================================================================
    /**
     * Given a received packet, process it.  If the destination for the packet
     * is this host, then deliver its data to the client layer.  If the
     * destination is another host, route and send the packet.
     *
     * @param packet The received packet to process.
     * @see   createPacket
     */
    protected void processPacket (byte[] packet) {
   // System.out.println("process");

	// first method called by receiver. reads intended destination
    int packetDestination = (int)((packet[1] & 0xff) << 24 |
    (packet[2] & 0xff) << 16 |
    (packet[3] & 0xff) << 8  |
    (packet[4] & 0xff));

    //assuming the packet has reached its final destination:
    if(packetDestination == address){
        //target will store the data (with no metadata)
        byte[] target = new byte[packet.length-9];
        for(int i=0; i<target.length; i++){
            target[i] = packet[i+9];
        }
        //send the data to the client
        client.receive(target);
        return;
    }
    //otherwise, send the packet over the chosen data link
    route(packetDestination).send(packet);
    
    } // processPacket ()
    // =========================================================================
    


    // =========================================================================
    // INSTANCE DATA MEMBERS

    /** The random source for selecting routes. */
    private Random random;
    // =========================================================================



    // =========================================================================
    // CLASS DATA MEMBERS

    /** The offset into the header for the length. */
    public static final int     lengthOffset      = 0;

    /** The offset into the header for the source address. */
    public static final int     sourceOffset      = lengthOffset + Integer.BYTES;

    /** The offset into the header for the destination address. */
    public static final int     destinationOffset = sourceOffset + Integer.BYTES;

    /** How many total bytes per header. */
    public static final int     bytesPerHeader    = destinationOffset + Integer.BYTES;

    /** Whether to emit debugging information. */
    public static final boolean debug             = false;
   // =========================================================================


    
// =============================================================================
} // class RandomNetworkLayer
// =============================================================================
