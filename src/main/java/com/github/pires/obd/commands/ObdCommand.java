package com.github.pires.obd.commands;

import com.github.pires.obd.exceptions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Base OBD command.
 *
 * @author pires
 * @version $Id: $Id
 */
public abstract class ObdCommand {

    /**
     * Error classes to be tested in order
     */
    private final Class[] ERROR_CLASSES = {
            UnableToConnectException.class,
            BusInitException.class,
            MisunderstoodCommandException.class,
            NoDataException.class,
            StoppedException.class,
            UnknownErrorException.class,
            UnsupportedCommandException.class
    };
    protected ArrayList<Integer> buffer = null;
    protected String cmd = null;
    protected boolean useImperialUnits = false;
    protected String rawData = null;
    protected Long responseDelayInMs = null;
    private long start;
    private long end;
    private static HashMap<String, Boolean> commandSupportedMap = new HashMap<String, Boolean>();
    
    public enum CommandSupportStatus {
    	Unknown,
    	NotSupported,
    	Supported
    };
    
    public static CommandSupportStatus getCommandSupportStatus(ObdCommand command) {
    	Boolean isCommandSupported = commandSupportedMap.get(command.cmd);

    	if (isCommandSupported == null) {
    		System.out.println("*** DBGMSG *** isCommandSupported: Unknown status for " + command.cmd);
    		return CommandSupportStatus.Unknown;
    	}
    	if (isCommandSupported == Boolean.TRUE) {
    		System.out.println("*** DBGMSG *** isCommandSupported: Result is true for " + command.cmd);
    		return CommandSupportStatus.Supported;
    	}

    	return CommandSupportStatus.NotSupported;
    }
    
    public static void setPIDSupported(int mode, int pid, Boolean isSupported) {
    	String key = String.format("%02X %02X", mode, pid);
    	commandSupportedMap.put(key, isSupported);
    }
    
    public static void clearPIDSupported() {
    	commandSupportedMap.clear();
    }

    public String getRawCommandString() {
    	return cmd;
    }

    /**
     * Default ctor to use
     *
     * @param command the command to send
     */
    public ObdCommand(String command) {
        this.cmd = command;
        this.buffer = new ArrayList<>();
    }

    /**
     * Prevent empty instantiation
     */
    private ObdCommand() {
    }

    /**
     * Copy ctor.
     *
     * @param other the ObdCommand to copy.
     */
    public ObdCommand(ObdCommand other) {
        this(other.cmd);
    }

    /**
     * Sends the OBD-II request and deals with the response.
     * <p>
     * This method CAN be overriden in fake commands.
     *
     * @param in  a {@link java.io.InputStream} object.
     * @param out a {@link java.io.OutputStream} object.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    public void run(InputStream in, OutputStream out) throws IOException,
            InterruptedException {
        synchronized (ObdCommand.class) {//Only one command can write and read a data in one time.
            start = System.currentTimeMillis();
            // Skip any remnant bytes from the stream
            for (int n = in.available(); n > 0; n = in.available()) {
            	System.err.println("*** DBGMSG *** Skipping remnant bytes " + n);
            	in.skip((long)n);
            }
            
            sendCommand(out);
            readResult(in);
            end = System.currentTimeMillis();
        }
    }

    /**
     * Sends the OBD-II request.
     * <p>
     * This method may be overriden in subclasses, such as ObMultiCommand or
     * TroubleCodesCommand.
     *
     * @param out The output stream.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    protected void sendCommand(OutputStream out) throws IOException,
            InterruptedException {
        // write to OutputStream (i.e.: a BluetoothSocket) with an added
        // Carriage return
        out.write((cmd + "\r").getBytes());
        out.flush();
        if (responseDelayInMs != null && responseDelayInMs > 0) {
            Thread.sleep(responseDelayInMs);
        }
    }

    /**
     * Resends this command.
     *
     * @param out a {@link java.io.OutputStream} object.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    protected void resendCommand(OutputStream out) throws IOException,
            InterruptedException {
        out.write("\r".getBytes());
        out.flush();
        if (responseDelayInMs != null && responseDelayInMs > 0) {
            Thread.sleep(responseDelayInMs);
        }
    }

    /**
     * Reads the OBD-II response.
     * <p>
     * This method may be overriden in subclasses, such as ObdMultiCommand.
     *
     * @param in a {@link java.io.InputStream} object.
     * @throws java.io.IOException if any.
     */
    protected void readResult(InputStream in) throws IOException {
        readRawData(in);
        checkForErrors();
        fillBuffer();
        performCalculations();
    }

    /**
     * This method exists so that for each command, there must be a method that is
     * called only once to perform calculations.
     */
    protected abstract void performCalculations();

    /**
     * <p>fillBuffer.</p>
     */
    protected void fillBuffer() {
        rawData = rawData.replaceAll("\\s", ""); //removes all [ \t\n\x0B\f\r]
        rawData = rawData.replaceAll("(BUS INIT)|(BUSINIT)|(\\.)", "");

        if (!rawData.matches("([0-9A-F])+")) {
            throw new NonNumericResponseException(rawData);
        }

        // read string each two chars
        buffer.clear();
        int begin = 0;
        int end = 2;
        while (end <= rawData.length()) {
            buffer.add(Integer.decode("0x" + rawData.substring(begin, end)));
            begin = end;
            end += 2;
        }
    }

    /**
     * <p>
     * readRawData.</p>
     *
     * @param in a {@link java.io.InputStream} object.
     * @throws java.io.IOException if any.
     */
    protected void readRawData(InputStream in) throws IOException {
        byte b = 0;
        StringBuilder res = new StringBuilder();

        /*
        // Wait for data to arrive
        try {
            System.err.println("***DEBUG***: Data not available in stream");
	        if (in.available() <= 0) {
	        	System.err.println("***DEBUG***: Data not available in stream");
	        	for (int waitCounter = 5;
	        			(waitCounter > 0) && (in.available() <= 0); 
	        			--waitCounter) {
	        		System.err.println("***DEBUG***: Data not available in stream. Count Down.. #" + waitCounter);
	        		Thread.sleep(100);
	        	}
	        }
            System.err.println("***DEBUG***: There is " + in.available() + " bytes in the BT stream");
        }
	    catch (Exception e) {
            e.printStackTrace();
	    }
	    */
        // read until '>' arrives OR end of stream reached
        // TODO: Implement a timeout logic, so that if the device
        // does not respond as expected (no '>') this does not wait forever
        char c;
        // -1 if the end of the stream is reached
        while (((b = (byte) in.read()) != -1)) {
            if (b < 0) { // Skip negative values
                continue;
            }
            c = (char) b;
            if (c == '>') // read until '>' arrives
            {
                break;
            }
            res.append(c);
        }

    /*
     * Imagine the following response 41 0c 00 0d.
     *
     * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
     * attention to the fact that I've put the word byte in quotes, because 41
     * is actually TWO bytes (two chars) in the socket. So, we must do some more
     * processing..
     */
        rawData = res.toString().replaceAll("SEARCHING", "");

    /*
     * Data may have echo or informative text like "INIT BUS..." or similar.
     * The response ends with two carriage return characters. So we need to take
     * everything from the last carriage return before those two (trimmed above).
     */
        //kills multiline.. rawData = rawData.substring(rawData.lastIndexOf(13) + 1);
        rawData = rawData.replaceAll("\\s", "");//removes all [ \t\n\x0B\f\r]
    }

    void checkForErrors() {
        for (Class<? extends ResponseException> errorClass : ERROR_CLASSES) {
            ResponseException messageError;

            try {
                messageError = errorClass.newInstance();
                messageError.setCommand(this.cmd);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if (messageError.isError(rawData)) {
                throw messageError;
            }
        }
    }

    /**
     * <p>getResult.</p>
     *
     * @return the raw command response in string representation.
     */
    public String getResult() {
        return rawData;
    }

    /**
     * <p>getFormattedResult.</p>
     *
     * @return a formatted command response in string representation.
     */
    public abstract String getFormattedResult();

    /**
     * <p>getCalculatedResult.</p>
     *
     * @return the command response in string representation, without formatting.
     */
    public abstract String getCalculatedResult();

    /**
     * <p>Getter for the field <code>buffer</code>.</p>
     *
     * @return a list of integers
     */
    protected ArrayList<Integer> getBuffer() {
        return buffer;
    }

    /**
     * <p>useImperialUnits.</p>
     *
     * @return true if imperial units are used, or false otherwise
     */
    public boolean useImperialUnits() {
        return useImperialUnits;
    }

    /**
     * The unit of the result, as used in {@link #getFormattedResult()}
     *
     * @return a String representing a unit or "", never null
     */
    public String getResultUnit() {
        return "";//no unit by default
    }

    /**
     * Set to 'true' if you want to use imperial units, false otherwise. By
     * default this value is set to 'false'.
     *
     * @param isImperial a boolean.
     */
    public void useImperialUnits(boolean isImperial) {
        this.useImperialUnits = isImperial;
    }

    /**
     * <p>getName.</p>
     *
     * @return the OBD command name.
     */
    public abstract String getName();

    /**
     * Time the command waits before returning from #sendCommand()
     *
     * @return delay in ms (may be null)
     */
    public Long getResponseTimeDelay() {
        return responseDelayInMs;
    }

    /**
     * Time the command waits before returning from #sendCommand()
     *
     * @param responseDelayInMs a Long (can be null)
     */
    public void setResponseTimeDelay(Long responseDelayInMs) {
        this.responseDelayInMs = responseDelayInMs;
    }

    //fixme resultunit
    /**
     * <p>Getter for the field <code>start</code>.</p>
     *
     * @return a long.
     */
    public long getStart() {
        return start;
    }

    /**
     * <p>Setter for the field <code>start</code>.</p>
     *
     * @param start a long.
     */
    public void setStart(long start) {
        this.start = start;
    }

    /**
     * <p>Getter for the field <code>end</code>.</p>
     *
     * @return a long.
     */
    public long getEnd() {
        return end;
    }

    /**
     * <p>Setter for the field <code>end</code>.</p>
     *
     * @param end a long.
     */
    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * <p>getCommandPID.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0-RC12
     */
    public final String getCommandPID() {
        return cmd.substring(3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObdCommand that = (ObdCommand) o;

        return cmd != null ? cmd.equals(that.cmd) : that.cmd == null;
    }

    @Override
    public int hashCode() {
        return cmd != null ? cmd.hashCode() : 0;
    }

}
