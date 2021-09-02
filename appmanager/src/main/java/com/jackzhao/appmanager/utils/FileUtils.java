package com.jackzhao.appmanager.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class FileUtils {
    private static final String TAG = FileUtils.class.getName();

    public static boolean chmod(File file, String mod) {
        try {
            Runtime.getRuntime().exec(String.format("chmod %s %s", mod, file.getAbsolutePath())).waitFor();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "chmod error: [" + file + "] " + mod);
            return false;
        }
    }

    public static void rmdirs(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    rmdirs(f);
                }
            }
        }
        file.delete();
    }


    public static void copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        try {
            String fileNames[] = context.getAssets().list(assetsPath);
            if (fileNames.length > 0) {
                File file = new File(savePath);
                file.mkdirs();
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {
                InputStream is = context.getAssets().open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "copyFilesFromAssets: " + e.getMessage());
        }
    }

    public static void replace(String path, String key, String newValue) {
        String temp = "";
        try {
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer buf = new StringBuffer();

            while ((temp = br.readLine()) != null) {

                buf = buf.append(temp.replace(key, newValue));
                buf = buf.append(System.getProperty("line.separator"));
            }

            br.close();
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            pw.write(buf.toString().toCharArray());
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static boolean copyfile(File fromFile, File toFile, boolean deleteOld) {

        if (!fromFile.exists()) {
            Log.e(TAG, "file not exist");
            return false;
        }

        if (!fromFile.isFile()) {
            Log.e(TAG, "file is not file");
            return false;
        }
        if (!fromFile.canRead()) {
            Log.e(TAG, "file can not read");
            return false;
        }
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (toFile.exists()) {
            toFile.delete();
        }

        FileInputStream fosfrom = null;
        FileOutputStream fosto = null;

        try {

            fosfrom = new FileInputStream(fromFile);
            fosto = new FileOutputStream(toFile);

            byte[] bt = new byte[1024];
            int c;
            while ((c = fosfrom.read(bt)) > 0) {
                fosto.write(bt, 0, c);
            }

            if (deleteOld) {
                fromFile.delete();
            }

            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            //关闭输入、输出流
            if (fosfrom != null) {
                try {
                    fosfrom.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fosto != null) {
                try {
                    fosto.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.e(TAG, "file copy failed");
        return false;
    }


    public static String readTxtFile(String strFilePath) {
        String path = strFilePath;
        StringBuffer content = new StringBuffer();
        File file = new File(path);
        if (file.isDirectory()) {
            Log.d("TestFile", "The File doesn't not exist.");
        } else {
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null) {
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;
                    while ((line = buffreader.readLine()) != null) {
                        content.append(line + System.getProperty("line.separator"));
                    }
                    instream.close();
                }
            } catch (FileNotFoundException e) {
                Log.d("TestFile", "The File doesn't not exist.");
            } catch (IOException e) {
                Log.d("TestFile", e.getMessage());
            }
        }
        return content.toString();
    }

    public static void writeFileData(String filename, String content) {
        try {
            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = content.getBytes();
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean isFileExist(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    public static String getFileLastLine(String path) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(new FileReader(path));
        String lastLine = null;
        while ((fileScanner.hasNextLine() && (lastLine = fileScanner.nextLine()) != null)) {
            if (!fileScanner.hasNextLine())
                Log.i(TAG, lastLine);
        }
        return lastLine;
    }

    public static String getAppDir(Context context) {
        if (context == null) {
            return null;
        }

        String dir = null;
        try {
            dir = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.dataDir;
        } catch (Exception e) {
            dir = context.getFilesDir().getParent();
        }
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir;
    }
}
