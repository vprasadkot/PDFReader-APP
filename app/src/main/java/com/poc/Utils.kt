package com.poc

import android.content.Context
import java.io.File
import java.io.InputStream
import androidx.pdf.PdfDocument
import java.io.FileWriter

class Utils {
//    fun extractPdfTextAndSaveToFile(context: Context, pdfInputStream: InputStream, outputFileName: String): File? {
//        return //try {
//            // Load the PDF
////            val document = pdfInputStream.use { PdfDocument.load(it) }
////
////            // Extract text
////            val stripper = PDFTextStripper()
////            val text = stripper.getText(document)
////            document.close()
////
////            // Write to output text file
////            val outputFile = File(context.filesDir, outputFileName)
////            FileWriter(outputFile).use { writer ->
////                writer.write(text)
////            }
////
////            outputFile
////        } catch (e: Exception) {
////            e.printStackTrace()
//            null
////        }
//
//    }
}