package at.jddev0.lang;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import at.jddev0.io.TerminalIO;

/**
 * Lang-Module<br>
 * Platform dependent code default implementation without showInputDialog() support
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class DefaultLangPlatformAPI implements ILangPlatformAPI {
    public DefaultLangPlatformAPI() {}

    //File methods
    public List<String> getLangFiles(String langPath) {
        List<String> files = new LinkedList<>();

        String[] in = new File(langPath).list();
        if(in != null) {
            for(String str:in) {
                File f = new File(langPath, str);
                if(!f.isDirectory() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".lang")) {
                    files.add(f.getPath());
                }
            }
        }

        return files;
    }

    public String getLangPath(String langFile) {
        File containingFolder = new File(langFile).getParentFile();
        if(containingFolder == null)
            containingFolder = new File("./");
        try {
            return containingFolder.getCanonicalPath();
        }catch(IOException e) {
            return containingFolder.getAbsolutePath();
        }
    }
    public String getLangFileName(String langFile) {
        return new File(langFile).getName();
    }

    public BufferedReader getLangReader(String langFile) throws IOException {
        return new BufferedReader(new FileReader(new File(langFile)));
    }

    public InputStream getInputStream(String langFile) throws IOException {
        return new FileInputStream(new File(langFile));
    }

    public boolean writeLangFile(File langFile, Map<String, String> translationMap, TerminalIO term) {
        try {
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8));

            for(String langRequest:translationMap.keySet()) {
                String value = translationMap.get(langRequest);
                //For multiline
                value = value.replace("\n", "\\n");

                w.write(langRequest + " = " + value);
                w.newLine();
            }

            w.close();
        }catch (IOException e) {
            term.logStackTrace(e, DefaultLangPlatformAPI.class);

            return false;
        }

        return true;
    }

    /**
     * This method is not implemented
     */
    public String showInputDialog(String text) throws Exception {
        throw new UnsupportedOperationException("Not Implemented");
    }
}