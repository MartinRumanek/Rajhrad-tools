package cz.mzk.rajhrad.tools;


import com.jcraft.jsch.*;
import org.apache.commons.io.IOUtils;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author hanis, Martin Rumanek
 */
public class Main {

    private static final String EXPORT_PATH = "/home/rumanekm/mzk03.m21";

    @Inject
    private Configuration configuration;

    @Inject
    private ImageserverUtils imageserverUtils;

    private String path;

    public Main() {
    }

    public void setPath(String path) {
        this.path = path;
    }

    private void run() {

        List<String> list = null;

        try {
            list = imageserverUtils.getFilenames(path);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RecordHolder holder = new RecordHolder(list);

        try {
            this.copyBase();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



        Validation validation = new Validation();
        validation.validate(holder, EXPORT_PATH);

       Client client = ClientBuilder.newBuilder().build();
        for (String url : holder.getImageserverLinkList()) {
            WebTarget webTarget = client.target(url + "/preview.jpg");
            Response response = webTarget.request().get();
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                System.err.println(url + " returned status code " + response.getStatus());
            } else {
               System.out.println(url + " is ok");
            }
        }




//
//        try {
//            imageserverUtils.uploadToImageserver(path);
//        } catch (JSchException e) {
//            e.printStackTrace();
//        } catch (SftpException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        System.out.println("------------------------------");
        holder.writeImageserverScript();
        System.out.println("------------------------------");
        holder.writeAlephScript();


    }

    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        Main rt = container.instance().select(Main.class).get();
        if (args.length >= 1) {
            rt.setPath(args[0]);
            rt.run();
        }
        weld.shutdown();
    }

    private void copyBase() throws JSchException, SftpException, IOException {

        File file = new File(EXPORT_PATH);

        if (file.exists()) {
            DateTime modifiedDate = new DateTime(file.lastModified());
            if (modifiedDate.withTimeAtStartOfDay().isAfter(new DateTime().minusDays(1).withTimeAtStartOfDay())) {
                //base on export path is already updated
                return;
            }
        } else {
            file.createNewFile();
        }

        JSch jsch = new JSch();
        Session session;
        if (configuration.getPasswordMarcExport() == null) {
            jsch.addIdentity(configuration.getPathPrivateKey(), configuration.getPrivateKeypassphrase());
            session = jsch.getSession(configuration.getSshUserMarcExport(), configuration.getSshHostMarcExport(), 22);
        } else {
            session = jsch.getSession(configuration.getSshUserMarcExport(), configuration.getSshHostMarcExport(), 22);
            session.setPassword(configuration.getPasswordMarcExport());
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp c = (ChannelSftp) channel;

        InputStream is = c.get(configuration.getPathMarcExport());

        try (FileOutputStream out = new FileOutputStream(EXPORT_PATH)) {
            IOUtils.copy(is, out);
        }

    }




}
