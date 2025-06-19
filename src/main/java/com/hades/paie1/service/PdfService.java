package com.hades.paie1.service;

import com.hades.paie1.dto.BulletinPaieResponseDto;
import com.lowagie.text.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import org.thymeleaf.TemplateEngine; // <--- Ajoute cette ligne si elle est manquante
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfService {

    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine){
        this.templateEngine = templateEngine;
    }

    public byte [] generateBulletinPdf (BulletinPaieResponseDto bulletinData) throws DocumentException, IOException{

        Context context = new Context();
        context.setVariable("bulletin",bulletinData);

        String html = templateEngine.process("bulletin-template",context);

        Document document = Jsoup.parse(html);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

        ITextRenderer renderer = new ITextRenderer();

        W3CDom w3CDom = new W3CDom();
        org.w3c.dom.Document w3cDocument = w3CDom.fromJsoup(document);

        renderer.setDocument(w3cDocument, null);
        renderer.layout();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }



}
