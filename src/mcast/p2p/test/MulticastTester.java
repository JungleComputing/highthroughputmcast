package mcast.p2p.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.poolInfo.PoolInfo;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

import mcast.p2p.Collective;
import mcast.p2p.Config;
import mcast.p2p.LocationPool;
import mcast.p2p.Pool;
import mcast.p2p.RankPool;
import mcast.p2p.storage.ByteArrayStorage;
import mcast.p2p.storage.FakeStorage;
import mcast.p2p.storage.RandomAccessFileStorage;
import mcast.p2p.storage.Storage;
import mcast.p2p.storage.StripedByteArrayStorage;
import mcast.p2p.util.Convert;

import org.apache.log4j.Logger;

import clusteremulation.ClusterEmulation;
import clusteremulation.EmulationScript;

public class MulticastTester implements Config {

	private static Logger logger = Logger.getLogger(MulticastTester.class);

	private static final String HR = "----------------------------------------------------------------------------";

	private static final String OPTION_TEST = "-test";
	private static final String OPTION_DATA_SIZE = "-data";
	private static final String OPTION_TIMES = "-times";
	private static final String OPTION_ENV = "-env";
	private static final String OPTION_USE_CLUSTER_EMULATOR = "-use-cluster-emulator";
	private static final String OPTION_FILE = "-file";
	private static final String OPTION_VALIDATE = "-validate-storage";
	private static final String OPTION_FAKE = "-fake-storage";
    private static final String OPTION_FILL = "-fill-storage";
    private static final String OPTION_TELL_BEFORE = "-tell-before";
	private static final String OPTION_TELL_AFTER = "-tell-after";
	private static final String OPTION_PIECE_SIZE = "-pieces";
    
	private enum Test { BITTORRENT, ROBBER };

	private static final IbisCapabilities REQ_CAPABILITIES = 
	    new IbisCapabilities(IbisCapabilities.CLOSED_WORLD);
	
	/**
	 * Array size limit of the IBM JIT
	 */
	private static final int MAX_ARRAY_SIZE = 1024 * 1024 * 255; 

	private Ibis ibis;
	private Pool appPool;
	private PoolInfo pool;
	private List<MulticastTest> tests;
	private boolean meHub;
	private Random random;
	private AckChannel appAckChannel;
	private AckChannel everybodyAckChannel;
	private EmulationScript emulationScript;
	private ClusterEmulation emulation;
	
	public MulticastTester(String testNames, String envFile,
			boolean useClusterEmulator) throws Exception {
        tests = new ArrayList<MulticastTest>();

	    logger.info("Creating pool ...");
		pool = new PoolInfo(null, true);
		meHub = false;
		
		if (envFile != null) {
			logger.info("Reading emulation script " + envFile);
			emulationScript = new EmulationScript(new File(envFile));

			if (useClusterEmulator) {
				logger.info("Creating cluster emulation");
				emulation = new ClusterEmulation(pool, emulationScript);
				meHub = emulation.meHub();
			} else {
				emulation = null;
			}
		}

		logger.info("Creating tests");
        ArrayList<PortType> portTypeList = new ArrayList<PortType>();
		for (String testName : testNames.split(",")) {
		    MulticastTest test = createTest(testName, meHub);
		    tests.add(test);
		    portTypeList.add(test.getPortType());
        }

        logger.info("Starting emulation script");
        Thread t = new Thread(emulationScript, "EmulationScript");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

		logger.info("Creating Ibis");
		PortType[] portTypes = portTypeList.toArray(new PortType[0]);
		ibis = IbisFactory.createIbis(REQ_CAPABILITIES, null, portTypes);
		
		logger.info("Waiting until everybody joined");
		Registry registry = ibis.registry();
		registry.waitUntilPoolClosed();
        IbisIdentifier[] everybody = registry.joinedIbises();
        int myRank = Arrays.asList(everybody).indexOf(ibis.identifier());
		
        logger.info("My rank: " + myRank);
        
		logger.info("Creating pool");
		if (useClusterEmulator) {
		    String[] clusterNames = emulationScript.getClusterNames();
		    appPool = new RankPool("app", ibis, clusterNames, myRank);
		} else {
		    appPool = new LocationPool("app", everybody);		    
		}
		
		if (meHub) {
		    logger.info("I'm a hub node for the cluster emulation");
        } else {    
            Collective myCollective = appPool.getCollective(ibis.identifier());
            logger.info("I'm an application node in collective " + myCollective);
			logger.info("Application pool: " + appPool);

			appAckChannel = new AckChannel(ibis, appPool);
		} 

		if (emulation == null) {
			// all nodes are application nodes
			everybodyAckChannel = appAckChannel;
		} else {
		    Pool everybodyPool = new LocationPool("everybody", everybody);
			everybodyAckChannel = new AckChannel(ibis, everybodyPool);
		}

		random = new Random(230979);
	}

	private MulticastTest createTest(String testName, boolean meHub) {
	    if (meHub) {
            // add dummy tests, so a hub node has the same number of test
            // objects and can stay in sync with potential acknowledgements 
            // during testing.
	        return new DummyMulticastTest();
	    }
        
	    Test test = Test.valueOf(testName);

	    switch(test) {
	    case BITTORRENT:
	        return new BitTorrentMulticastTest(testName);
	    case ROBBER:
	        return new RobberMulticastTest(testName);
	    }

	    throw new RuntimeException("unknown test: " + testName);
	}

	private String formatSeconds(long nanosec) {
		double sec = Convert.nanosecToSec(nanosec);
		return Convert.round(sec, 2) + " sec.";
	}

	private String formatThroughput(long nanosec, long bytesSent) {
		double sec = Convert.nanosecToSec(nanosec);
		double mbytesPerSec = Convert
		.bytesPerSecToMBytesPerSec(bytesSent / sec);
		return Convert.round(mbytesPerSec, 2) + " MB/s";
	}

	private Storage createStorage(int bytes, File file, boolean fake, 
	        int pieceSize, boolean fill) {
		logger.info("generating storage of "
				+ Convert.round(Convert.bytesToMBytes(bytes), 2) + " MB...");

		if (file != null) {
			logger.info("creating file storage");

			if (fill) {
				logger
				.warn("you requested a filled storage, but we won't overwrite "
						+ file.getAbsolutePath());
			}

			try {
				return new RandomAccessFileStorage(file, bytes, pieceSize, false);
			} catch (IOException e) {
				throw new RuntimeException("could not create file storage", e);
			}
		} else if (fake) {
			logger.info("creating fake storage");

			if (fill) {
				throw new RuntimeException(
				"you requested a filled storage, but a fake storage cannot be filled");
			}

			return new FakeStorage(bytes, pieceSize);
		} else if (bytes <= MAX_ARRAY_SIZE) {
			// create a simple MemoryStorage
			logger.info("creating simple memory storage");

			byte[] data = new byte[bytes];
			if (fill) {
				fillStripe(data);
			}
			return new ByteArrayStorage(data, 0, bytes, pieceSize);
		} else {
			// create a StripedMemoryStorage
			logger.info("creating striped memory storage");

			int noStripes = (int) Math.ceil(bytes / (double) MAX_ARRAY_SIZE);
			byte[][] data = new byte[noStripes][];

			for (int i = 0; i < noStripes - 1; i++) {
				data[i] = new byte[MAX_ARRAY_SIZE];
				if (fill) {
					fillStripe(data[i]);
				}
			}
			if (noStripes > 0) {
				int lastStripeLength = bytes
				- ((noStripes - 1) * MAX_ARRAY_SIZE);
				data[noStripes - 1] = new byte[lastStripeLength];
			}
			if (fill) {
				fillStripe(data[noStripes - 1]);
			}

			return new StripedByteArrayStorage(data, pieceSize);
		}
	}

	private void fillStripe(byte[] stripe) {
		logger.info("filling storage with random bytes...");

		for (int i = 0; i < stripe.length / 10; i++) {
			stripe[i * 10] = (byte) random.nextInt(256);
		}
	}

	public void run(int times, int size, int pieceSize,
	        File file, boolean fake, boolean fill, boolean validate, 
	        String tellBefore, String tellAfter)
	throws IOException, ParseException, NoSuchAlgorithmException {
		Storage storage = null;
		byte[] storageDigest = null;

		logger.info(HR);

		IbisIdentifier me = ibis.identifier();
        IbisIdentifier root = appPool.getEverybody().get(0);

        logger.info("Me:   " + me);

		if (!meHub) {
			logger.info("Root: " + root);
			logger.info("Multicasting " + times + " times "
					+ Convert.bytesToMBytes(size) + " MB");

			if (file == null) {
				logger.info("Data storage: memory");
			} else {
				logger.info("Data storage: file " + file.getAbsolutePath());
			}

			logger.info("Methods used: " + tests.toString());
			logger.info("Piece size:   " + pieceSize);
			logger.info(HR);

			boolean fillStorage = validate || fill;
			storage = createStorage(size, file, fake, pieceSize, fillStorage);
			storageDigest = validate ? storage.getDigest() : null;

			if (validate && size < 20) {
				logger.info("Multicast buffer: " + storage);
			}
		} else {
			logger.info("Running hub node");
		}

		Set<IbisIdentifier> roots = Collections.singleton(root);

		// run tests
		for (int i = 0; i < times; i++) {
			for (Iterator<MulticastTest> it = tests.iterator(); it.hasNext();) {
				MulticastTest test = it.next();

				// clear all existing data in the storage at all application 
				// nodes except the root
				if (!meHub && !me.equals(root)) {
					storage.clear();
				}

				logger.info("Synching all nodes before test");
				everybodyAckChannel.acknowledge();

				// tell the cluster emulator to start the next test,
				// including all the hub nodes
				if (tellBefore != null && emulationScript != null) {
					logger.info("Telling emulation script before test '" + 
					        tellBefore + "'");
					emulationScript.tell(tellBefore);
				}

				long[] nanosec = null;

				if (!meHub) {
					logger.info("Run " + (i + 1) + " - " + times + " of "
							+ test.getName());
					nanosec = test.timeMulticast(storage, roots);
				}

				if (tellAfter != null && emulationScript != null) {
					logger.info("Synching all nodes after test");
					everybodyAckChannel.acknowledge();

					logger.info("Telling emulation script after test'" + 
					        tellAfter + "'");
					emulationScript.tell(tellAfter);
				}

				if (!meHub) {
					if (validate) {
						logger.info("Validating multicast data...");
						byte[] multicastDigest = storage.getDigest();
						if (Arrays.equals(storageDigest, multicastDigest)) {
							logger.info("Validated multicast data: OK");
						} else {
							logger.fatal("Validated multicast data: GARBLED!");
							if (size < 20) {
								logger.fatal("Multicast buffer: " + storage);
							}
						}
					}

					if (nanosec.length > 1) {
						// print the time it took until we received everything
						long time = nanosec[0];

						logger.info(me + " " + test.getName() + "_received_"
								+ (i + 1) + ": " + formatSeconds(time) + " = "
								+ formatThroughput(time, size));
					}

					if (me.equals(root)) {
						if (nanosec.length > 0) {
							// print the time it took until everybody received
							// everything
							long time = nanosec[nanosec.length - 1];

							logger.info(test.getName() + "_multicast_" + 
							        (i + 1) + ": " + formatSeconds(time) + 
							        " = " + formatThroughput(time, size));
						}
					}

					test.printStats();

					logger.info(HR);
				}
			}
		}
	}

	public void end() throws IOException {
		logger.info("Synching all nodes for the last time");
		everybodyAckChannel.acknowledge();

		if (emulationScript != null) {
			logger.info("Ending emulation script");
			emulationScript.end();
		}

		logger.info("Ending multicast tests");
		for (Iterator<MulticastTest> it = tests.iterator(); it.hasNext();) {
			MulticastTest test = it.next();
			test.close();
		}

		logger.info("Ending Ibis");
		ibis.end();

		// wait a while for all the Ibis updates to propagate over the
		// SmartSockets hubs...
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ignored) {
			// ignore
		}

		// kill the hubs
		if (emulation != null) {
			logger.info("Ending cluster emulation");
			emulation.end();
		}
	}

	private static String createOption(String name, String value) {
		return " [" + name + " " + value + "]";
	}

	enum BYTE_UNITS { MB, KB, MBit, Kbit };
    private static void usage() {
        String byteUnits = EnumSet.allOf(BYTE_UNITS.class).toString();
        String testOptions = EnumSet.allOf(Test.class).toString();
		System.err.println("usage:");
		System.err.println("  java MulticastTest" + 
		        createOption(OPTION_TIMES, "<no. times>") +
				createOption(OPTION_DATA_SIZE, "<size>" + byteUnits) +
				createOption(OPTION_TEST, testOptions) +
				createOption(OPTION_VALIDATE, ""));
		System.exit(1);
	}

	private static int parseSize(String name, String value) {
		try {
			return (int) Convert.parseBytes(value);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("we need an integer " + name
					+ " followed by a unit (MB, KB, MBit, Kbit)");
			usage();
		}
		return 0; // NEVER REACHED
	}

	private static int parseInt(String name, String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("we need an integer " + name);
			usage();
		}
		return 0; // NEVER REACHED
	}

	private static boolean parseBoolean(String name, String value) {
		try {
			return Boolean.parseBoolean(value);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("we need an boolean " + name);
			usage();
		}
		return false; // NEVER REACHED
	}

	public static void main(String[] argv) {
		logger.info("Java version: " + System.getProperty("java.vm.vendor")
				+ " " + System.getProperty("java.vm.version"));
		logger.info("Classpath: " + System.getProperty("java.class.path"));

		int times = 1;
		int dataSize = 100; // bytes
		String testNames = "";
		String envFile = null;
		boolean useClusterEmulator = true;
		File dataFile = null;
		boolean validate = false;
		boolean fake = false;
		boolean fill = false;
        String tellBefore = null;
		String tellAfter = null;
		int pieceSize = 32 * 1024;

		try {
			for (int i = 0; i < argv.length; i++) {
				if (false) {
				} else if (argv[i].equals(OPTION_TIMES)) {
					times = parseInt("times", argv[++i]);
				} else if (argv[i].equals(OPTION_DATA_SIZE)) {
					dataSize = parseSize("data size", argv[++i]);
                } else if (argv[i].equals(OPTION_PIECE_SIZE)) {
                    pieceSize = parseSize("piece size", argv[++i]);
				} else if (argv[i].equals(OPTION_TEST)) {
					testNames = argv[++i];
				} else if (argv[i].equals(OPTION_ENV)) {
					envFile = argv[++i];
				} else if (argv[i].equals(OPTION_USE_CLUSTER_EMULATOR)) {
					useClusterEmulator = parseBoolean(
							"enabled/disable cluster emulator", argv[++i]);
				} else if (argv[i].equals(OPTION_FILE)) {
					dataFile = new File(argv[++i]);
                } else if (argv[i].equals(OPTION_FAKE)) {
                    fake = parseBoolean("fake storage", argv[++i]);
                } else if (argv[i].equals(OPTION_FILL)) {
                    fill = parseBoolean("fill storage", argv[++i]);
                } else if (argv[i].equals(OPTION_VALIDATE)) {
                    validate = parseBoolean("validate storage", argv[++i]);
                } else if (argv[i].equals(OPTION_TELL_BEFORE)) {
					tellBefore = argv[++i];
				} else if (argv[i].equals(OPTION_TELL_AFTER)) {
					tellAfter = argv[++i];
				} else {
					System.err.println("unknown option: " + argv[i]);
					usage();
				}
			}

			MulticastTester test = new MulticastTester(testNames, envFile, 
			        useClusterEmulator);
			test.run(times, dataSize, pieceSize, dataFile, fake, fill, 
			        validate, tellBefore, tellAfter);
			test.end();
		} catch (Throwable e) {
			e.printStackTrace();
		}

		logger.info("Done");
	}

}
