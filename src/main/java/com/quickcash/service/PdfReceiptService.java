package com.quickcash.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import com.quickcash.dto.ReceiptResponse;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfReceiptService {

    public byte[] generateReceiptPdf(
            ReceiptResponse receipt
    ) {

        try {

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document =
                    new Document();

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            18
                    );

            Font headingFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            12
                    );

            document.add(
                    new Paragraph(
                            "Smart Connection Pvt. Ltd.",
                            titleFont
                    )
            );

            document.add(
                    new Paragraph(
                            "QuickCash Payment Receipt",
                            headingFont
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            document.add(
                    new Paragraph(
                            "Receipt Number: "
                                    + receipt.getReceiptNumber()
                    )
            );

            document.add(
                    new Paragraph(
                            "Loan Code: "
                                    + receipt.getLoanCode()
                    )
            );

            document.add(
                    new Paragraph(
                            "Borrower Code: "
                                    + receipt.getBorrowerCode()
                    )
            );

            document.add(
                    new Paragraph(
                            "Borrower Name: "
                                    + receipt.getBorrowerName()
                    )
            );

            document.add(
                    new Paragraph(
                            "Payment Code: "
                                    + receipt.getPaymentCode()
                    )
            );

            document.add(
                    new Paragraph(
                            "Payment Amount: LKR "
                                    + receipt.getAmount()
                    )
            );

            document.add(
                    new Paragraph(
                            "Outstanding Balance: LKR "
                                    + receipt.getBalanceAfter()
                    )
            );

            document.add(
                    new Paragraph(
                            "Payment Date: "
                                    + receipt.getPaymentDate()
                    )
            );

            document.add(
                    new Paragraph(
                            "Collector: "
                                    + receipt.getCollectorName()
                    )
            );

            document.add(
                    new Paragraph(
                            "Receipt Issued: "
                                    + receipt.getIssuedAt()
                    )
            );

            document.add(
                    new Paragraph(" ")
            );

            document.add(
                    new Paragraph(
                            "Thank you for your payment."
                    )
            );

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate PDF receipt",
                    e
            );
        }
    }
}
