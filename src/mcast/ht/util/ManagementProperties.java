package mcast.ht.util;

import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;

public class ManagementProperties {

	private static final String MPROP_MESSAGE_BYTES = "MessageBytes";
	
	// N.B. we do not refer to Managable anymore, since this breaks the use
	// of the latest Ibis trunk (where is has been renamed to Manageable)
	// We therefore had to copy-paste several methods.
	
	public static long getMessageBytes(SendPort p) {
		try {
			String bytes = p.getManagementProperty(MPROP_MESSAGE_BYTES);
			return Long.parseLong(bytes);
		} catch (NoSuchPropertyException e) {
			throw new RuntimeException("Management property '" + 
					MPROP_MESSAGE_BYTES + " cannot be read", e);
		}
	}
	
    public static long getMessageBytes(ReceivePort p) {
        try {
            String bytes = p.getManagementProperty(MPROP_MESSAGE_BYTES);
            return Long.parseLong(bytes);
        } catch (NoSuchPropertyException e) {
            throw new RuntimeException("Management property '" + 
                    MPROP_MESSAGE_BYTES + " cannot be read", e);
        }
    }

    public static void setMessageBytes(SendPort p, long value) {
		try {
			p.setManagementProperty(MPROP_MESSAGE_BYTES, String.valueOf(value));
		} catch (NoSuchPropertyException e) {
			throw new RuntimeException("Management property '" + 
					MPROP_MESSAGE_BYTES + " cannot be set", e);
		}
	}

    public static void setMessageBytes(ReceivePort p, long value) {
        try {
            p.setManagementProperty(MPROP_MESSAGE_BYTES, String.valueOf(value));
        } catch (NoSuchPropertyException e) {
            throw new RuntimeException("Management property '" + 
                    MPROP_MESSAGE_BYTES + " cannot be set", e);
        }
    }

}
