package com.example.pentaho.utils;

import com.example.pentaho.component.Directory;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;


public class QRCodeUtils {

    private final static Logger log = LoggerFactory.getLogger(QRCodeUtils.class);

    private static final int QRCODE_SIZE =300;

    private final static String CHARSET = "UTF-8";
    private final static boolean COMPRESS_IMG = true;

    private final static int LOGO_WIDTH = 60;

    private final static int LOGO_HEIGHT = 60;

    private final static String FILE_PREFIX = "qrcode_";

    private final static String FILE_FORMAT_TYPE ="JPG";


    private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");



    public final static void insertImage(BufferedImage image,String logoPath,boolean compressImg) throws IOException {
        File file = new File(logoPath);
        if(!file.exists()){
            log.info("logo 不存在!!!");
            return;
        }

        Image src = ImageIO.read(new File(logoPath));
        int height = src.getHeight(null);
        int width = src.getWidth(null);
        if(compressImg){
            if(height > LOGO_HEIGHT){
                height =LOGO_HEIGHT;
            }

            if(width > LOGO_WIDTH){
                width = LOGO_WIDTH;
            }
            Image scaledImage = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics graphics = bufferedImage.getGraphics();
            graphics.drawImage(scaledImage,0,0,null);
            graphics.dispose();
            src = scaledImage;
        }

        Graphics2D graph = image.createGraphics();
        int x = (QRCODE_SIZE - width) / 2;
        int y = (QRCODE_SIZE - height) / 2;
        graph.drawImage(src,x,y,width,height,null);
        Shape shape  = new RoundRectangle2D.Float(x, y, width, width, 6, 6);
        graph.setStroke(new BasicStroke(3f));
        graph.draw(shape);
        graph.dispose();
    }

    public final static BufferedImage createImage(String content,String logoPath) throws WriterException, IOException {
        log.info("圖片位置:{}",logoPath);
        log.info("是否壓縮:{}",COMPRESS_IMG);
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET,CHARSET);
        hints.put(EncodeHintType.MARGIN,1);
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, hints);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for(int x= 0;x<width;x++){
          for(int y=0;y<height;y++){
              image.setRGB(x,y,bitMatrix.get(x,y) ? 0xFF000000:0xFFFFFFFF);
          }
        }
          //logo不存在，直接回returnimage
          if(StringUtils.isNullOrEmpty(logoPath)){
              return image;
          }

          //insert image
          insertImage(image,logoPath,COMPRESS_IMG);
        return image;
    }



    private final static void encode(BufferedImage bufferedImage,String qrcodePath) throws IOException {
        log.info("qrcode jpg 絕對位置:{}",qrcodePath);
        ImageIO.write(bufferedImage,FILE_FORMAT_TYPE,new File(qrcodePath));
    }

    public final static void generateQrcode(String content,String logoPath,String qrcodePath) throws IOException, WriterException {
        BufferedImage image = createImage(content,logoPath);
        encode(image,qrcodePath);
    }

}
