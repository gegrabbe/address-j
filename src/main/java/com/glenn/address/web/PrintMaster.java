package com.glenn.address.web;

import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.MongoService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/print")
public class PrintMaster {
    private static final Logger logger = LoggerFactory.getLogger(PrintMaster.class);
    public static final String ADDRESS_BOOK_PDF = "address-book.pdf";
    private final MongoService mongoService;

    // Define colors - black and white theme
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(240, 240, 240); // Light gray background

    @SuppressWarnings("unused")
    public PrintMaster() {
        this.mongoService = new MongoService("input-data.json");
    }

    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    @SuppressWarnings("unused")
    public ResponseEntity<byte[]> printAllEntries() {
        logger.debug("#### printAllEntries ####");
        try {
            List<Entry> entries = AddressApi.sortById(mongoService.readFromDatabase());
            byte[] pdfBytes = generatePdf(entries);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(ADDRESS_BOOK_PDF)
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Failed to generate PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private byte[] generatePdf(List<Entry> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Add title
        Paragraph title = new Paragraph("Address Book Manager")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Add entry count
        Paragraph count = new Paragraph("Total Entries: " + entries.size())
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(count);

        // Create cards for each entry - 2 cards per page
        int cardCount = 0;
        for (Entry entry : entries) {
            document.add(createEntryCard(entry));
            cardCount++;

            // Add page break after every 2 cards (but not after the last card)
            if (cardCount % 2 == 0 && cardCount < entries.size()) {
                document.add(new com.itextpdf.layout.element.AreaBreak(com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private Table createEntryCard(Entry entry) {
        Table card = new Table(UnitValue.createPercentArray(new float[]{1}));
        card.setWidth(UnitValue.createPercentValue(100));
        card.setMarginBottom(15);
        card.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

        // Header section with ID and Name - spans full width
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}));
        header.setWidth(UnitValue.createPercentValue(100));
        header.setBackgroundColor(LIGHT_BG);

        // Entry ID
        Cell idCell = new Cell()
                .add(new Paragraph("ID: " + entry.entryId())
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
        header.addCell(idCell);

        // Name
        String fullName = entry.person().firstName() + " " + entry.person().lastName();
        Cell nameCell = new Cell()
                .add(new Paragraph(fullName)
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
        header.addCell(nameCell);

        Cell headerContainer = new Cell()
                .add(header)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
        card.addCell(headerContainer);

        // Two-column layout for Person Details (left) and Contact Information (right)
        Table twoColumns = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        twoColumns.setWidth(UnitValue.createPercentValue(100));

        // Left column - Person Details
        Table leftColumn = new Table(UnitValue.createPercentArray(new float[]{1}));
        leftColumn.setWidth(UnitValue.createPercentValue(100));

        if (entry.person().age() != null || entry.person().gender() != null || entry.person().maritalStatus() != null) {
            leftColumn.addCell(createSectionTitle("Person Details"));

            if (entry.person().age() != null) {
                leftColumn.addCell(createDetailRow("Age:", String.valueOf(entry.person().age())));
            }
            if (entry.person().gender() != null) {
                leftColumn.addCell(createDetailRow("Gender:", entry.person().gender().toString()));
            }
            if (entry.person().maritalStatus() != null) {
                leftColumn.addCell(createDetailRow("Marital Status:", entry.person().maritalStatus().toString()));
            }
        }

        Cell leftCell = new Cell()
                .add(leftColumn)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
        twoColumns.addCell(leftCell);

        // Right column - Contact Information
        Table rightColumn = new Table(UnitValue.createPercentArray(new float[]{1}));
        rightColumn.setWidth(UnitValue.createPercentValue(100));

        if (hasContactInfo(entry)) {
            rightColumn.addCell(createSectionTitle("Contact Information"));

            if (entry.address().street() != null) {
                rightColumn.addCell(createDetailRow("Street:", entry.address().street()));
            }
            if (entry.address().city() != null || entry.address().state() != null || entry.address().zip() != null) {
                String location = buildLocation(entry);
                rightColumn.addCell(createDetailRow("Location:", location));
            }
            if (entry.address().email() != null) {
                rightColumn.addCell(createDetailRow("Email:", entry.address().email()));
            }
            if (entry.address().phone() != null) {
                rightColumn.addCell(createDetailRow("Phone:", entry.address().phone()));
            }
        }

        Cell rightCell = new Cell()
                .add(rightColumn)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
        twoColumns.addCell(rightCell);

        // Add the two-column layout to the card
        Cell twoColumnsContainer = new Cell()
                .add(twoColumns)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
        card.addCell(twoColumnsContainer);

        // Notes Section - spans full width
        if (entry.notes() != null && !entry.notes().isEmpty()) {
            card.addCell(createSectionTitle("Notes"));
            Cell notesCell = new Cell()
                    .add(new Paragraph(entry.notes())
                            .setFontSize(10)
                            .setItalic())
                    .setBackgroundColor(LIGHT_BG)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(10);
            card.addCell(notesCell);
        }

        return card;
    }

    private Cell createSectionTitle(String title) {
        return new Cell()
                .add(new Paragraph(title)
                        .setFontSize(11)
                        .setBold()
                        .setFontColor(ColorConstants.BLACK))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(10)
                .setPaddingBottom(5)
                .setPaddingLeft(10);
    }

    private Cell createDetailRow(String label, String value) {
        Paragraph p = new Paragraph()
                .add(new Paragraph(label)
                        .setFontSize(10)
                        .setBold()
                        .setMarginRight(5))
                .add(new Paragraph(value)
                        .setFontSize(10));

        return new Cell()
                .add(p)
                .setBorder(Border.NO_BORDER)
                .setPaddingLeft(10)
                .setPaddingTop(2)
                .setPaddingBottom(2);
    }

    private boolean hasContactInfo(Entry entry) {
        return entry.address().street() != null ||
                entry.address().city() != null ||
                entry.address().state() != null ||
                entry.address().zip() != null ||
                entry.address().email() != null ||
                entry.address().phone() != null;
    }

    private String buildLocation(Entry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.address().city() != null) {
            sb.append(entry.address().city());
        }
        if (entry.address().state() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(entry.address().state());
        }
        if (entry.address().zip() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(entry.address().zip());
        }
        return sb.toString();
    }
}
