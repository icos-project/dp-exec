/*
 *  Copyright 2002-2024 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package es.bsc.compss.agent.rest;

import es.bsc.compss.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public final class ZenohMonitorLauncher {

    private static final int BUFFER_SIZE = 4_096;
    private static final Logger LOGGER = LogManager.getLogger(Loggers.AGENT);
    private static final String SCRIPT_PATH;

    static {
        String compssHome = File.separator + "opt" + File.separator + "COMPSs" + File.separator;
        ;
        String scriptPath = "Runtime" + File.separator + "scripts" + File.separator + "utils" + File.separator;
        String scriptName = "zenoh_monitor.py";
        SCRIPT_PATH = compssHome + scriptPath + scriptName;
    }

    private static Process monitor;


    private ZenohMonitorLauncher() {
        // Disabling instance creations
    }

    /**
     * Starts the monitoring from the Zenoh router.
     * 
     * @throws IOException the process could not be started.
     */
    public static synchronized void startMonitor() throws IOException {
        if (monitor != null && monitor.isAlive()) {
            LOGGER.info("Zenoh Monitor already started");
            return;
        }
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("python3", SCRIPT_PATH);
        monitor = pb.start();
        monitor.getOutputStream().close();
        new Gobbler(monitor.getInputStream(), System.out).start();
        new Gobbler(monitor.getErrorStream(), System.err).start();

    }


    private static class Gobbler extends Thread {

        private final PrintStream out;
        private final InputStream in;


        public Gobbler(InputStream in, PrintStream out) {
            this.out = out;
            this.in = in;
        }

        public void run() {
            try {
                final byte[] buffer = new byte[BUFFER_SIZE];
                int nRead;
                while ((nRead = in.read(buffer, 0, buffer.length)) != -1) {
                    byte[] readData = new byte[nRead];
                    if (out != null) {
                        System.arraycopy(buffer, 0, readData, 0, nRead);
                        String data = new String(readData);
                        out.print(data);
                        out.flush();
                    }
                }
            } catch (IOException ioe) {
                LOGGER.error("Exception during reading/writing in output Stream", ioe);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                        LOGGER.warn("Exception closing IN InputStream", ioe);
                    }
                }

                if (out != null) {
                    out.flush();
                    // out.close();
                }
            }
        }
    }


    /**
     * Stops the monitoring from the Zenoh router.
     * 
     * @throws IOException the process could not be stopped.
     */
    public static synchronized void stopMonitor() {
        if (monitor != null && monitor.isAlive()) {
            monitor.destroyForcibly();
            try {
                monitor.waitFor();
            } catch (Exception e) {
                // Waiting for end
            }
        }
        monitor = null;
    }
}
