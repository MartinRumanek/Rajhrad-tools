package cz.mzk.rajhrad.tools;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.IOUtils;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.marc4j.MarcException;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hanis
 */
public class Main {

    private static final String EXPORT_PATH = "/home/rumanekm/mzk03.m21";

    @Inject
    Configuration configuration;

    public Main() {
    }

    private void run() {

        List<String> list = null;
        try {
            list = getFilenames();
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //List<String> list = createSetFromFile("/home/hanis/projects/rajhrad/ST/Q/filenames.txt");
        RecordHolder holder = new RecordHolder(list);


        InputStream in = null;
        int exc = 0;
        int i = 0;
        int j = 0;
        int k = 0;
        try {
            in = new FileInputStream(EXPORT_PATH);
            MarcReader reader = new MarcPermissiveStreamReader(in, true, true, "UTF-8");
            while (reader.hasNext()) {
                try {
                    Record record = reader.next();

                    String sysno = getSingleSubfield(record, "990", "a");
                    //System.out.println(sysno);
                    String barcode = getSingleSubfield(record, "980", "1");
                    holder.handleRecord(sysno, barcode);
                } catch (MarcException e) {
                    j++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    // System.out.println("EXCEPTION: " + ++exc);

                }
            }
            in.close();
            in = new FileInputStream(EXPORT_PATH);
            reader = new MarcPermissiveStreamReader(in, true, true, "UTF-8");
            while (reader.hasNext()) {
                try {
                    Record record = reader.next();
                    String lkr = getSingleSubfield(record, "LKR", "a");
                    if ("UP".equals(lkr)) {

                        String sysno = getSingleSubfield(record, "990", "a");
                        String mainSysno = getSingleSubfield(record, "LKR", "b");
                        String supplementName = getSingleSubfield(record, "LKR", "m");
                        //System.out.println(sysno + ", " + mainSysno + ", " + supplementName);
                        holder.handleSupplement(sysno, mainSysno, supplementName);
                    }
                } catch (MarcException e) {
                    j++;
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("X");
                }
            }
            in.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

        }
//        System.out.println(i + ", " + j + ", " + k);
//
//        for (RecordRelation rr : holder.getRecordRelations()) {
//            System.out.println(rr);
//        }
//        for (String b : holder.getBarcodes()) {
//            System.out.println(b);
//        }
//        System.out.println("-----------------------------");
//
//

        holder.divideAll();
        int c = 0;


        System.out.println("<h2>Varovani: Prebyvaji privazky</h2>");
        for (RecordRelation rr : holder.getRecordRelations()) {
            if (!rr.isOk() && rr.getConflictType() == RecordRelation.SUPPLEMENT_NUMBER_CONFLICT &&
                    rr.getNumberOfExcpectedSupplements() < rr.getSupplements().size()) {
                c++;
                System.out.println("<h3>Varovani: ocekavany pocet privazku: <b>" + rr.getNumberOfExcpectedSupplements()
                        + "</b> skutecny pocet privazku: <b>" + rr.getSupplements().size() + "</b></h3><p>" + rr.toHtml() + "</p>");
            }
        }

        System.out.println("Celkem: " + c + "<br/>");
        c = 0;
        System.out.println("<h2>Chyby: Chybi privazky</h2>");
        for (RecordRelation rr : holder.getRecordRelations()) {
            if (!rr.isOk() && rr.getConflictType() == RecordRelation.SUPPLEMENT_NUMBER_CONFLICT &&
                    rr.getNumberOfExcpectedSupplements() > rr.getSupplements().size()) {
                c++;
                System.out.println("<h3>Chyba: ocekavany pocet privazku: <b>" + rr.getNumberOfExcpectedSupplements()
                        + "</b> skutecny pocet privazku: <b>" + rr.getSupplements().size() + "</b></h3><p>" + rr.toHtml() + "</p>");
            }
        }
        System.out.println("Celkem: " + c + "<br/>");


        System.out.println("<h2>Chyby v nazvech privazku</h2>");
        c = 0;
        for (RecordRelation rr : holder.getRecordRelations()) {
            if (!rr.isOk() && rr.getConflictType() == RecordRelation.SUPPLEMENT_NAME_CONFLICT) {
                c++;
                System.out.println(rr.getSysno() + "<br/>");
            }
        }
        System.out.println("Celkem: " + c + "<br/>");
        c = 0;
        System.out.println("<h2>Nezname chyby</h2>");
        for (RecordRelation rr : holder.getRecordRelations()) {
            if (!rr.isOk() && rr.getConflictType() == RecordRelation.UNKNOWN_CONFLICT) {
                c++;
                System.out.println(rr.getSysno() + "<br/>");
            }
        }
        System.out.println("Celkem: " + c + "<br/>");


//        System.out.println("-----------------------------------------------");
//        c = 0;
//        for (RecordRelation rr : holder.getRecordRelations()) {
//            if (rr.isOk()) {
//                c++;
//              //  System.out.println(rr.getSysno());
//            }
//        }
//        System.out.println("dobre celkem: " + c);
//
//
//
//        System.out.println("correct count: " + holder.getCorrectCount());
//        System.out.println("incorrect count: " + holder.getIncorrectCount());
//
        System.out.println("------------------------------");
        holder.writeImageserverScript();
        System.out.println("------------------------------");
        holder.writeAlephScript();


    }

    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        Main rt = container.instance().select(Main.class).get();
        weld.shutdown();
        rt.run();
    }

    private void copyBase() throws JSchException, SftpException, IOException {

        JSch jsch = new JSch();
        Session session = jsch.getSession(configuration.getSshHostWorkspace(), "zeus.mzk.cz", 22);
        session.setPassword(configuration.getSshPasswordWorkspace());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp c = (ChannelSftp) channel;

        InputStream is = c.get(configuration.getPathMarcExport());


        try (FileOutputStream out = new FileOutputStream("/home/rumanekm/mzk03.m21")) {
            IOUtils.copy(is, out);
        }

    }

    private List<String> getFilenames() throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();

        Session session = jsch.getSession(configuration.getSshUserWorkspace(), configuration.getSshHostWorkspace(), 22);
        session.setPassword(configuration.getSshPasswordWorkspace());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();

        ChannelSftp c = (ChannelSftp) channel;

        ArrayList<ChannelSftp.LsEntry> list = new ArrayList<ChannelSftp.LsEntry>(c.ls("/rajhrad/Rajhrad/IMAGESERVER/mapy_rajhrad"));

        List<String> filenames = new ArrayList<String>();

        for(ChannelSftp.LsEntry entry : list) {
            filenames.add(entry.getFilename());
        }
        filenames.remove(".");
        filenames.remove("..");

        return filenames;
    }


    private String getSingleSubfield(Record record, String field, String subfield) {
        DataField dataField = (DataField) record.getVariableField(field);
        if (dataField == null) {
            return "";
        }
        Subfield dataSubfield = dataField.getSubfield(subfield.charAt(0));
        if (dataSubfield == null) {
            return "";
        }
        return dataSubfield.getData();
    }
}
