package mcast.ht.util;

public class Convert {

	public static double toSec(double value, String unit)
	throws IllegalArgumentException
	{
		unit = unit.trim();

		if (unit.equals("s") || unit.equals("sec") || unit.equals("seconds")) {
			return value;
		} else if (unit.equals("ms") || unit.equals("msec")
				|| unit.equals("millisec") || unit.equals("milliseconds")) {
			return millisecToSec(value);
		} else {
			throw new IllegalArgumentException("cannot convert " + unit
					+ " to seconds");
		}
	}

	public static double toBytes(double value, String unit)
	throws IllegalArgumentException
	{
		unit = unit.trim();

		if (unit.equals("B") || unit.equals("bytes")) {
			return value;
		} else if (unit.equals("KB") || unit.equals("KiB")
				|| unit.equalsIgnoreCase("kbyte")
				|| unit.equalsIgnoreCase("kbytes") || unit.equals("kilobytes")) {
			return kbytesToBytes(value);
		} else if (unit.equals("MB") || unit.equals("MiB")
				|| unit.equalsIgnoreCase("mbyte")
				|| unit.equalsIgnoreCase("mbytes")
				|| unit.equalsIgnoreCase("megabytes")) {
			return mbytesToBytes(value);
		} else if (unit.equalsIgnoreCase("kbit")
				|| unit.equalsIgnoreCase("kbits")
				|| unit.equalsIgnoreCase("kilobits")) {
			return kbitsToBytes(value);
		} else if (unit.equalsIgnoreCase("mbit")
				|| unit.equalsIgnoreCase("mbits")
				|| unit.equalsIgnoreCase("megabits")) {
			return mbitsToBytes(value);
		} else if (unit.equalsIgnoreCase("GB") || unit.equalsIgnoreCase("GiB")) {
			return gbytesToBytes(value);
		} else if (unit.equalsIgnoreCase("gbit")
				|| unit.equalsIgnoreCase("gbits")
				|| unit.equalsIgnoreCase("gigabit")
				|| unit.equalsIgnoreCase("gigabits")) {
			return gbitsToBytes(value);
		} else {
			throw new IllegalArgumentException("cannot convert " + unit
					+ " to bytes");
		}
	}

	public static double toBytesPerSec(double value, String unit)
	throws IllegalArgumentException
	{
		unit = unit.trim();

		int slashIndex = unit.indexOf('/');

		if (slashIndex < 0) {
			throw new IllegalArgumentException("missing '/' in unit " + unit);
		} else if (slashIndex == 0) {
			throw new IllegalArgumentException("missing data unit in " + unit);
		} else if (slashIndex == unit.length() - 1) {
			throw new IllegalArgumentException("missing time unit in " + unit);
		}

		String dataUnit = unit.substring(0, slashIndex);
		String timeUnit = unit.substring(slashIndex + 1);

		double result = toBytes(value, dataUnit);
		result /= toSec(1.0, timeUnit);

		return result;
	}

	public static double parseNumber(String number) {
		int i = 0;
		while (i < number.length()
				&& (Character.isDigit(number.charAt(i)) || 
					number.charAt(i) == '.')) {
			i++;
		}
		return Double.parseDouble(number.substring(0, i));
	}

	public static String parseUnit(String value) {
		int i = 0;
		while (i < value.length()
				&& (Character.isDigit(value.charAt(i)) || 
					value.charAt(i) == '.')) {
			i++;
		}
		return value.substring(i);
	}

	public static double parseBytes(String s) {
		double number = parseNumber(s);
		String unit = parseUnit(s);
		return toBytes(number, unit);
	}

	public static double parseSec(String s) {
		double number = parseNumber(s);
		String unit = parseUnit(s);
		return toSec(number, unit);
	}

	public static double parseBytesPerSec(String s) {
		double number = parseNumber(s);
		String unit = parseUnit(s);
		return toBytesPerSec(number, unit);
	}

	public static long mbitsToBytes(double mbits) {
		return (long) (mbits * 1024 * 1024 / 8);
	}

	public static double mbitsToMBytes(double mbits) {
		return mbits / 8.0;
	}

	public static double mbytesToBytes(double mbytes) {
		return mbytes * 1024 * 1024;
	}

	public static double gbytesToBytes(double gbytes) {
		return gbytes * 1024 * 1024 * 1024;
	}

	public static double gbitsToBytes(double gbits) {
		return gbits * 1024 * 1024 * 1024 / 8;
	}

	public static long mbitsPerSecToBytesPerMillisec(double mbitsPerSec) {
		return (long) (mbitsToBytes(mbitsPerSec) / 1000.0);
	}

	public static double mbytesPerSecToBytesPerSec(double mbytesPerSec) {
		return mbytesPerSec * 1024.0 * 1024.0;
	}

	public static long mbytesPerSecToBytesPerMillisec(double mbytesPerSec) {
		return (long) (mbytesToBytes(mbytesPerSec) / 1000.0);
	}

	public static long mbytesPerSecToBytesPerNanosec(double mbytesPerSec) {
		return (long) (mbytesToBytes(mbytesPerSec) / 1000000000.0);
	}

	public static double bytesPerSecToKBytesPerSec(double bytesPerSec) {
		return bytesPerSec / 1024.0;
	}

	public static double bytesPerSecToMBytesPerSec(double bytesPerSec) {
		return bytesPerSec / 1024.0 / 1024.0;
	}

	public static double bytesPerSecToBytesPerNanosec(double bytesPerSec) {
		return bytesPerSec / 1000.0 / 1000.0 / 1000.0;
	}

	public static double bytesPerSecToBytesPerMillisec(double bytesPerSec) {
		return bytesPerSec / 1000.0;
	}

	public static double bytesPerMillisecToBytesPerSec(double bytesPerMillisec)
	{
		return bytesPerMillisec * 1000.0;
	}

	public static double bytesPerNanosecToBytesPerSec(double bytesPerNanosec) {
		return bytesPerNanosec * 1000000000.0;
	}

	public static double bytesPerNanosecToKBytesPerSec(double bytesPerNanosec) {
		return bytesPerNanosec * 1000000000.0 / 1024.0;
	}

	public static double bytesPerNanosecToMBytesPerSec(double bytesPerNanosec) {
		return bytesPerNanosec * 1000000000.0 / 1024.0 / 1024.0;
	}

	public static double bytesToMbits(int bytes) {
		return bytes / 1024.0 / 1024.0 * 8;
	}

	public static double bytesToMbits(long bytes) {
		return bytes / 1024.0 / 1024.0 * 8;
	}

	public static double bytesToMBytes(int bytes) {
		return bytes / 1024.0 / 1024.0;
	}

	public static double bytesToMBytes(long bytes) {
		return bytes / 1024.0 / 1024.0;
	}

	public static double bytesToMBytes(double bytes) {
		return bytes / 1024.0 / 1024.0;
	}

	public static double bytesToKBytes(long bytes) {
		return bytes / 1024.0;
	}

	public static double mbytesToMbits(double mbytes) {
		return mbytes * 8;
	}

	public static double kbytesToMbits(double kbytes) {
		return kbytes / 1024.0 * 8;
	}

	public static int kbytesToBytes(double kbytes) {
		return (int) (Math.round(kbytes * 1024));
	}

	public static double kbytesToMBytes(double kbytes) {
		return kbytes / 1024.0;
	}

	public static double kbitsToMbits(double kbits) {
		return kbits / 1024.0;
	}

	public static double kbitsToMBytes(double kbits) {
		return kbits / 1024.0 / 8.0;
	}

	public static double kbitsToBytes(double kbits) {
		return kbits / 8.0 * 1024.0;
	}

	public static double millisecToSec(long msec) {
		return msec / 1000.0;
	}

	public static double millisecToSec(double msec) {
		return msec / 1000.0;
	}

	public static long millisecToNanosec(double msec) {
		return (long) (msec * 1000 * 1000);
	}

	public static long secToMillisec(double sec) {
		return (long) (sec * 1000);
	}

	public static long secToNanosec(double sec) {
		return (long) (sec * 1000000000L);
	}

	public static double microsecToSec(double microsec) {
		return microsec / 1000000L;
	}

	public static double microsecToMillisec(double microsec) {
		return microsec / 1000;
	}

	public static double nanosecToSec(double nanosec) {
		return nanosec / 1000000000L;
	}

	public static double nanosecToMillisec(double nanosec) {
		return Math.floor(nanosec / 1000000L);
	}

	public static byte booleanToByte(boolean b) {
		return b ? (byte) 1 : (byte) 0;
	}

	public static boolean byteToBoolean(byte b) {
		return (b == 0) ? false : true;
	}
	
	public static byte[] booleansToBytes(boolean[] booleans) {
	    int size = booleans.length >> 3;
        int rest = booleans.length % 8;
        
        if (rest > 0) {
            size++;
        }
        
        byte[] bytes = new byte[size];        
        
        int byteIndex = 0;
        int bitCount = 0;
        
        for (int i = 0; i < booleans.length; i++) {
            if (bitCount > 0) {
                bytes[byteIndex] = (byte)(bytes[byteIndex] << 1);
            }
            
            if (booleans[i]) {
                bytes[byteIndex] |= 1;
            }
            
            bitCount++;
            if (bitCount == 8) {
                bitCount = 0;
                byteIndex++;
            }
        }
        
        if (rest > 0) {
            int lastIndex = bytes.length - 1;
            bytes[lastIndex] = (byte)(bytes[lastIndex] << (8 - rest));
        }
        
        return bytes;
	}

    private static final byte[] masks = { -128, 64, 32, 16, 8, 4, 2, 1 };
    
	public static boolean[] bytesToBooleans(byte[] bytes, int booleanArrSize) {
	    boolean[] booleans = new boolean[booleanArrSize];
	        
	    int byteIndex = 0;
	    int bitCount = 0;
	        
	    for (int i = 0; i < booleanArrSize; i++) {
	        if ((bytes[byteIndex] & masks[bitCount]) != 0) {
	            booleans[i] = true;
	        }
	            
	        bitCount++;
	            
	        if (bitCount == 8) {
	            bitCount = 0;
	            byteIndex++;
	        }
	    }
	        
	    return booleans;
	}
	
}
