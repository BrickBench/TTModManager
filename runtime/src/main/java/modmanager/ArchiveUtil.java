package modmanager;

import modmanager.ui.BottomPane;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import java.io.*;

public class ArchiveUtil {
    private static boolean init = false;

    public static void extract(String path, String target) {
        if(!init){
            try {
                SevenZip.initSevenZipFromPlatformJAR();
                BottomPane.log("7-Zip-JBinding library was initialized");
            } catch (SevenZipNativeInitializationException e) {
                e.printStackTrace();
            }
            init = true;
        }

        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;
        try {
            randomAccessFile = new RandomAccessFile(new File(path), "r");
            inArchive = SevenZip.openInArchive(null,
                    new RandomAccessFileInStream(randomAccessFile));

            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                if (!item.isFolder()) {
                    var newFile = new File(target + "\\" + item.getPath());
                    newFile.getParentFile().mkdirs();
                    newFile.delete();
                    var writer = new BufferedOutputStream(new FileOutputStream(newFile));

                    item.extractSlow(data -> {
                        try {
                            writer.write(data);
                            return data.length;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    });

                    writer.flush();
                    writer.close();
                }
            }
        } catch (Exception e) {
            BottomPane.log("Error occurs: " + e);
        } finally {
            if (inArchive != null) {
                try {
                    inArchive.close();
                } catch (SevenZipException e) {
                    BottomPane.log("Error closing archive: " + e);
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    BottomPane.log("Error closing file: " + e);
                }
            }
        }
    }
}
