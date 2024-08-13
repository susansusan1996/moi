package com.example.pentaho.utils;

import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component
public class JasperResportUtils {

    final static Logger log = LoggerFactory.getLogger(JasperResportUtils.class);



    public final static JasperPrint compileReport(String filePath, Map<String,Object> paramaters, Collection<?> fields) throws JRException, IOException, NoSuchMethodException {
        /*當找到當前加載類別，在以此找出檔案相對位置並書獲取輸出流*/
        String path = Thread.currentThread().getClass().getResource(filePath).toString();
        log.info("path:{}",path);
        JasperReport jasperReport = JasperCompileManager.compileReport(Thread.currentThread().getClass().getResource(filePath).openStream());
        return bindData(jasperReport, paramaters,fields);
    }
//
    public final static JasperPrint bindData(JasperReport jasperReport, Map<String,Object> paramaters,Collection<?> beanCollection) throws JRException {
        if (!beanCollection.isEmpty()) {
            JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(beanCollection);
            return JasperFillManager.fillReport(jasperReport, paramaters, source);
        } else {
            JREmptyDataSource source = new JREmptyDataSource();
            return JasperFillManager.fillReport(jasperReport, paramaters, source);
        }
    }
//
    public final static void exporterOdsFile( String fileName,JasperPrint jasperPrint, HttpServletResponse response) throws JRException, IOException {
        SimpleOdsReportConfiguration odsReportConfiguration = new SimpleOdsReportConfiguration();
        JROdsExporter jrOdsExporter = new JROdsExporter();
        jrOdsExporter.setConfiguration(odsReportConfiguration);
        jrOdsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        response.setHeader("Content-Disposition", "attachment;filename="+fileName);
        response.setContentType("application/octet-stream");
        jrOdsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
        jrOdsExporter.exportReport();
    }
//
    public final static void exportExcelFile(String fileName, JasperPrint jasperPrint, HttpServletResponse response) throws IOException, JRException {
        SimpleXlsxExporterConfiguration simpleXlsxExporterConfiguration = new SimpleXlsxExporterConfiguration();
        JRXlsxExporter jrXlsxExporter = new JRXlsxExporter();
        jrXlsxExporter.setConfiguration(simpleXlsxExporterConfiguration);
        jrXlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        response.setHeader("Content-Type","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename="+fileName);
        jrXlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
        jrXlsxExporter.exportReport();
    }
//
    public final static void exporterPDFFile(String fileName,JasperPrint jasperPrint, HttpServletResponse response) throws JRException, IOException {
        SimplePdfExporterConfiguration simplePdfExporterConfiguration = new SimplePdfExporterConfiguration();
        JRPdfExporter jrPdfExporter = new JRPdfExporter();
        jrPdfExporter.setConfiguration(simplePdfExporterConfiguration);
        jrPdfExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        response.setHeader("Content-Disposition", "attachment;filename="+fileName);
        response.setContentType("application/pdf");
        jrPdfExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream()));
        jrPdfExporter.exportReport();
    }


}
