import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class DeployTomcat {

    public static void main(String[] a) throws IOException, InterruptedException {
        int build = run("./deply.sh"); 
        // e.g. 
        // mvn compile war:exploded -o -T1C -DskipTests
        // rm -rfd ../CATALINA_HOME/webapps/*
        // cp -R PROJECT/target/PROJECT ../CATALINA_HOME/webapps/

        if (build == 0) {
            System.setProperty("JPDA_SUSPEND", "y");
            String[] envp = {
                    "JPDA_TRANSPORT=dt_socket",
                    "JPDA_ADDRESS=8010",
                    "JPDA_SUSPEND=y"};
            run("./run.sh", envp, System.getenv("CATALINA_HOME") + "/bin");
            // e.g. export JPDA_TRANSPORT=dt_socket
            // export JPDA_ADDRESS=8010
            // export JPDA_SUSPEND=n # if you need Tomcat to wait for attached debugger from ide set value to "y"
            // export ENABLE_PROCESSING_TIMEOUT_FIRSTDATAMSIP_FEATURE_SWITCH=true
            // ./catalina.sh jpda run

        }
    }

    static int run(String command) throws IOException, InterruptedException {
        return run(command, null, null);
    }

    static int run(String command, String[] envp, String location) throws IOException, InterruptedException {
        File dir = null;
        if (location != null) {
            dir = new File(location);
        }
        Process p = Runtime.getRuntime().exec(command, envp, dir);
        new Thread(new SyncPipe(p.getErrorStream(), System.err)).start();
        new Thread(new SyncPipe(p.getInputStream(), System.out)).start();
        int returnCode = p.waitFor();
        System.out.println("Return code = " + returnCode);
        return returnCode;
    }

    static class SyncPipe implements Runnable {
        public SyncPipe(InputStream istrm, OutputStream ostrm) {
            istrm_ = istrm;
            ostrm_ = ostrm;
        }

        public void run() {
            try {
                final byte[] buffer = new byte[1024];
                for (int length = 0; (length = istrm_.read(buffer)) != -1; ) {
                    ostrm_.write(buffer, 0, length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private final OutputStream ostrm_;
        private final InputStream istrm_;
    }
}
