package com.github.pires.obd.commands.protocol;

import com.github.pires.obd.enums.AvailableCommandNames;

public class AvailablePidsCommand_Mode09_01_20 extends AvailablePidsCommand {

    /**
     * Default ctor.
     */
    public AvailablePidsCommand_Mode09_01_20() {
        super("09 00");
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.github.pires.obd.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand_Mode09_01_20(AvailablePidsCommand_Mode09_01_20 other) {
        super(other);
    }

    /** {@inheritDoc} */
	@Override
	public String getName() {
		return AvailableCommandNames.PIDS_MODE09_01_20.getValue();
	}
	
    /** {@inheritDoc} */
    @Override
    protected int getMode() {
    	return 0x09;
    }
    
    /** {@inheritDoc} */
    @Override
    protected int getStartPID() {
    	return 0x01;
    }
}
