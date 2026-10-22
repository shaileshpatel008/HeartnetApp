package in.dibc.kavitul.drcardio.SupportClass;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ECGReportGenerator {

    private Context context;
    private String ecgGifUrl;
    private String specialistName;
    private String impression;
    private String advice;
    private String signatureUrl;

    public ECGReportGenerator(Context context, String ecgGifUrl, String specialistName,
                              String impression, String advice, String signatureUrl) {
        this.context = context;
        this.ecgGifUrl = ecgGifUrl;
        this.specialistName = specialistName;
        this.impression = impression;
        this.advice = advice;
        this.signatureUrl = signatureUrl;
    }

    public void generateECGReport() {
        new GeneratePdfTask().execute();
    }

    private class GeneratePdfTask extends AsyncTask<Void, Void, File> {
        @Override
        protected File doInBackground(Void... voids) {
            try {
                // Create directory in Scoped Storage
                File documentsDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "heartnetapp");
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();
                }

                // Generate PDF file name
                String fileName = "ECG_Report_" + System.currentTimeMillis() + ".pdf";
                File pdfFile = new File(documentsDir, fileName);

                // Create PDF document
                PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
                PdfDocument pdfDocument = new PdfDocument(writer);
                Document document = new Document(pdfDocument);

                // Download and add ECG GIF
                Bitmap ecgBitmap = getBitmapFromURL(ecgGifUrl);
                if (ecgBitmap != null) {
                    ImageData imageData = ImageDataFactory.create(convertBitmapToByteArray(ecgBitmap));
                    Image image = new Image(imageData);
                    document.add(image);
                } else {
                    document.add(new Paragraph("ECG Image Not Available").setTextAlignment(TextAlignment.CENTER));
                }

                // Add Impression, Advice, Specialist Details
                document.add(new Paragraph("\nImpression & Advice:")
                        .setBold()
                        .setFontSize(14));
                document.add(new Paragraph(impression).setFontSize(12));
                document.add(new Paragraph(advice).setFontSize(12));

                // Add Specialist Name
                document.add(new Paragraph("\nReported by Specialist:")
                        .setBold()
                        .setFontSize(14));
                document.add(new Paragraph(specialistName).setFontSize(12));

                // Add Signature Image
                Bitmap signatureBitmap = getBitmapFromURL(signatureUrl);
                if (signatureBitmap != null) {
                    ImageData signatureData = ImageDataFactory.create(convertBitmapToByteArray(signatureBitmap));
                    Image signatureImage = new Image(signatureData);
                    signatureImage.setWidth(100).setHeight(50);
                    document.add(signatureImage);
                } else {
                    document.add(new Paragraph("Signature Not Available"));
                }

                // Close document
                document.close();
                return pdfFile;
            } catch (Exception e) {
                Log.e("ECGReport", "Error generating PDF", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File pdfFile) {
            if (pdfFile != null) {
                // Open the generated PDF
                openPDF(pdfFile);
            }
        }
    }

    // Function to get Bitmap from URL
    private Bitmap getBitmapFromURL(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("ECGReport", "Error loading image from URL", e);
            return null;
        }
    }

    // Convert Bitmap to Byte Array
    private byte[] convertBitmapToByteArray(Bitmap bitmap) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            Log.e("ECGReport", "Error converting Bitmap", e);
            return null;
        }
    }

    // Open PDF File
    private void openPDF(File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }



    public static class ECGReportGeneratorTask extends AsyncTask<Void, Void, Uri> {
        private final Context context;
        private final String ecgImageUrl;
        private final String impression;
        private final String advice;
        private final String specialistName;
        private final String signatureUrl;
        private ProgressDialog progressDialog;

        public ECGReportGeneratorTask(Context context, String ecgImageUrl, String impression, String advice, String specialistName, String signatureUrl) {
            this.context = context;
            this.ecgImageUrl = ecgImageUrl;
            this.impression = impression;
            this.advice = advice;
            this.specialistName = specialistName;
            this.signatureUrl = signatureUrl;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Generating report...");
            progressDialog.setCancelable(false); // Prevents dismissal on touch outside
            progressDialog.show();
        }

        @Override
        protected Uri doInBackground(Void... voids) {
            try {
                File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "heartnetapp");
                if (!dir.exists()) dir.mkdirs();

                String fileName = "ECG_Report_" + System.currentTimeMillis() + ".pdf";
                File pdfFile = new File(dir, fileName);

                // PDF settings: 11.68 x 8.26 inches in points (1 inch = 72 points)
                float width = 11.68f * 72;
                float height = 12f * 72;

                PdfWriter writer = new PdfWriter(pdfFile);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document doc = new Document(pdfDoc, new PageSize(width, height));

                // 1. Add GIF image
                Bitmap gifBitmap = BitmapFactory.decodeStream(new URL(ecgImageUrl).openStream());
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                gifBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                ImageData imageData = ImageDataFactory.create(stream.toByteArray());
                Image ecgImage = new Image(imageData);
                ecgImage.setAutoScale(true);
                doc.add(ecgImage);


                // 2. Impression and advice (left), Specialist name and signature (right)
                float columnWidth = width / 2f - 40;

                Paragraph leftText = new Paragraph()
                        .add(new Text("Impression and Advice :\n").setBold())
                        .add(impression + "\n" + advice)
                        .setFontSize(11);

                Paragraph rightText = new Paragraph()
                        .add(new Text("Reported by Specialist :\n").setBold())
                        .add(specialistName)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.RIGHT);

                // Signature
                if (signatureUrl != null && !signatureUrl.isEmpty()) {
                    Bitmap signBmp = BitmapFactory.decodeStream(new URL(signatureUrl).openStream());
                    ByteArrayOutputStream signStream = new ByteArrayOutputStream();
                    signBmp.compress(Bitmap.CompressFormat.PNG, 100, signStream);
                    ImageData signData = ImageDataFactory.create(signStream.toByteArray());
                    Image signature = new Image(signData);
                    signature.scaleToFit(100, 50);
                    rightText.add("\n").add(signature);
                }

                // Add both sides as columns
                Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                table.addCell(new Cell().add(leftText).setBorder(Border.NO_BORDER));
                table.addCell(new Cell().add(rightText).setBorder(Border.NO_BORDER));
                doc.add(table);

                LineSeparator separator = new LineSeparator(new SolidLine(0.6f)); // 1 dp height
                separator.setMarginTop(10);
                separator.setMarginBottom(10);
                doc.add(separator); // Add it before footer

                // 3. Footer
//                String footer = "Report done by Specialist as per data sent by Physician/Diagnostic Center, " +
//                        "Clinical correlation recommended\n10mm/mv,25.0mm/sec Heartnet asaan 7.9.6/4.";
//                doc.add(new Paragraph(footer)
//                        .setFontSize(10)
//                        .setTextAlignment(TextAlignment.CENTER)
//                        .setMarginTop(20));

                Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                footerTable.setMarginTop(0);

                footerTable.addCell(new Cell()
                        .add(new Paragraph("Report done by Specialist as per data sent by Physician/Diagnostic Center, Clinical correlation recommended"))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setFontSize(10));

                footerTable.addCell(new Cell()
                        .add(new Paragraph("10mm/mv,25.0mm/sec Heartnet asaan 7.9.6/4."))
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontSize(10));

                doc.add(footerTable);

                doc.close();
                return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", pdfFile);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            progressDialog.dismiss();
            if (uri != null) {
                // Open the PDF
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "PDF generation failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}






