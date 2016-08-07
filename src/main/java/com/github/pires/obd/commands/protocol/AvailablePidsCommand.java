package com.github.pires.obd.commands.protocol;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.PersistentCommand;

/**
 * Retrieve available PIDs ranging from 21 to 40.
 *
 * @author pires
 * @version $Id: $Id
 */
public abstract class AvailablePidsCommand extends PersistentCommand {

    /**
     * Default ctor.
     *
     * @param command a {@link java.lang.String} object.
     */
    public AvailablePidsCommand(String command) {
        super(command);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand(AvailablePidsCommand other) {
        super(other);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void performCalculations() {
    	// super.performCalculations();
    	String result = getCalculatedResult();
    	if (result.length() == 8) {
    		try {
	    		long bitFlags = Long.parseLong(result, 16);
	    		setSupportedPIDs(bitFlags, getMode(), getStartPID());
    		} catch (NumberFormatException e) {
    			System.out.println(e.toString());
    		}
    	}
    }	

    protected abstract int getMode();
    protected abstract int getStartPID();

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return getCalculatedResult();
    }

    /** {@inheritDoc} */
    @Override
    public String getCalculatedResult() {
        //First 4 characters are a copy of the command code, don't return those
        return String.valueOf(rawData).substring(4);
    }
    
    protected void setSupportedPIDs(long bitFlag, int mode, int startPid) {
    	String pidStr = Long.toBinaryString(bitFlag);
    	int startIndex = 32 - pidStr.length();
    	for (int i = 0; (i + startIndex) < 32; ++i) {
    		int pid = startPid + i + startIndex;
    		if (pidStr.charAt(i) == '1') {
    			ObdCommand.setPIDSupported(mode, pid, Boolean.TRUE);
    		} else {
    			ObdCommand.setPIDSupported(mode, pid, Boolean.FALSE);
    		}
    	}
    	/*
        for (int bitMask = 0x80000000, pid = startPid; bitMask != 0; ++pid, bitMask = bitMask >> 1) {
            if ((bitMask & bitFlag) == bitMask) {
            	ObdCommand.setPIDSupported(mode, pid, Boolean.TRUE);
            }
        }
        */
    }    
}
