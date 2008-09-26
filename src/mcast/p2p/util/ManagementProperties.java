package mcast.p2p.util;

import ibis.ipl.Managable;
import ibis.ipl.NoSuchPropertyException;

public class ManagementProperties {

	private static final String MPROP_MESSAGE_BYTES = "MessageBytes";
	
	public static int getMessageBytes(Managable m) {
		try {
			String bytes = m.getManagementProperty(MPROP_MESSAGE_BYTES);
			return Integer.parseInt(bytes);
		} catch (NoSuchPropertyException e) {
			throw new RuntimeException("Management property '" + 
					MPROP_MESSAGE_BYTES + " cannot be read", e);
		}
	}
	
	public static void setMessageBytes(Managable m, int value) {
		try {
			m.setManagementProperty(MPROP_MESSAGE_BYTES, String.valueOf(value));
		} catch (NoSuchPropertyException e) {
			throw new RuntimeException("Management property '" + 
					MPROP_MESSAGE_BYTES + " cannot be set", e);
		}
	}

}
