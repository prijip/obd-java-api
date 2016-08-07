package com.github.pires.obd.commands.protocol;

import com.github.pires.obd.enums.AvailableCommandNames;

/**
 * Retrieve available PIDs ranging from 01 to 20.
 *
 * @author pires
 * @version $Id: $Id
 */
public class AvailablePidsCommand_Mode02_41_60 extends AvailablePidsCommand {

    /**
     * Default ctor.
     */
    public AvailablePidsCommand_Mode02_41_60() {
        super("02 00");
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand_Mode02_41_60(AvailablePidsCommand_Mode02_41_60 other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.PIDS_MODE02_41_60.getValue();
    }
    
    /** {@inheritDoc} */
    @Override
    protected int getMode() {
    	return 0x02;
    }
    
    /** {@inheritDoc} */
    @Override
    protected int getStartPID() {
    	return 0x41;
    }
}
