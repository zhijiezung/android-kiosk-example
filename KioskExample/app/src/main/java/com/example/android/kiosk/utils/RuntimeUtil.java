package com.example.android.kiosk.utils;

import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RuntimeUtil {

    private static String resultMsg = "";

    public static String runCmd(String command) {
        Log.e("run CMD", command);
        Runtime run = Runtime.getRuntime();
        try {
            Process p = run.exec(command);
            InputStream ins = p.getInputStream();
            InputStream ers = p.getErrorStream();
            new Thread(new InputStreamThread(ins, false)).start();
            new Thread(new InputStreamThread(ers, true)).start();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

        }
        return resultMsg;
    }

    static class InputStreamThread implements Runnable {
        private InputStream ins = null;
        private boolean error;

        public InputStreamThread(InputStream ins, boolean error) {
            this.ins = ins;
            this.error = error;
        }

        /**
         * 换行，ASCII码 10，"\n"
         */
        @Override
        public void run() {
            try {
                String result = read(ins);
                resultMsg = result;

                if (error) {
                    if (!TextUtils.isEmpty(result)) {
                        Log.e("run error, result", result);
                    }
                } else {
                    Log.e("run normal, result", result);
                    /*if ("Success\n".equals(result)) {
                        LogUtils.e("CMD 执行成功！");
                    } else {
                        LogUtils.e("CMD 执行失败：", result);
                    }*/
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         *
         */
        public String read(InputStream is) throws Exception
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            // 将内容读到buffer中，读到末尾为-1
            while ((len = is.read(buffer)) != -1)
            {
                // 本例子将每次读到字节数组(buffer变量)内容写到内存缓冲区中，起到保存每次内容的作用
                outStream.write(buffer, 0, len);
            }
            byte[] data = outStream.toByteArray(); // 取内存中保存的数据
            is.close();
            outStream.close();
            String result = new String(data, "UTF-8");
            return result;
        }
    }
}
