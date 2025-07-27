package com.poc.pdfreader

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.poc.pdfreader.databinding.FragmentFirstBinding
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.IOException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex: Int = 0
    private var _binding: FragmentFirstBinding? = null
    private var currentPdfUri: Uri? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prevButton = binding.prevButton
        nextButton = binding.nextButton
        // Set up button click listeners
        prevButton.setOnClickListener {
            showPage(currentPageIndex - 1)
        }

        nextButton.setOnClickListener {
            showPage(currentPageIndex + 1)
        }
        binding.openPDF.setOnClickListener {
            pickPdfLauncher.launch(arrayOf("application/pdf"))
        }
    }
    private val pickPdfLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
           val flags = requireActivity().intent.flags and (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            requireContext().contentResolver.takePersistableUriPermission(it, flags)
            currentPdfUri = it
            openPdf(it) // Open the selected PDF file
        } ?: run {
            Toast.makeText(requireContext(), "No PDF file selected.", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Opens a PDF file from the assets folder.
     * Copies the PDF from assets to a temporary file, then uses its URI.
     */
    private fun openPdf(pdfUri: Uri) {
        try {
            currentPage?.close()
            pdfRenderer?.close()
            pdfRenderer = null // Reset renderer
            currentPage = null // Reset current page
            currentPageIndex = 0 // Reset page index
        } catch (e: IOException) {
            println("Error opening PDF: ${e.message}")
        }
        try {
            val fileDescriptor = requireContext().contentResolver.openFileDescriptor(pdfUri, "r")
            if (fileDescriptor != null) {
                // Initialize PdfRenderer with the file descriptor
                pdfRenderer = PdfRenderer(fileDescriptor)
                // Show the first page
                showPage(0)
            } else {
                Toast.makeText(requireContext(), "Failed to open PDF file descriptor from URI.", Toast.LENGTH_LONG).show()

            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error opening PDF from URI: ${e.message}", Toast.LENGTH_LONG).show()

        }
    }

    /**
     * Displays a specific page of the PDF.
     * @param index The index of the page to display (0-based).
     */
    private fun showPage(index: Int) {
        if (pdfRenderer == null || pdfRenderer?.pageCount == 0) {
            println("No PDF opened or no pages available.")
            return
        }

        // Close the current page if it's open
        currentPage?.close()

        // Ensure the index is within bounds
        val newIndex = when {
            index < 0 -> 0
            index >= pdfRenderer!!.pageCount -> pdfRenderer!!.pageCount - 1
            else -> index
        }
        currentPageIndex = newIndex

        try {
            // Open the new page
            currentPage = pdfRenderer!!.openPage(currentPageIndex)
            println("PRINT TEXT -- ${currentPage?.textContents}")
            // Create a bitmap to render the page onto

            // Render the page onto the bitmap

            // Perform text extraction on a background thread
            lifecycleScope.launch(Dispatchers.IO) {
                var extractedText: String? = "Failed to extract text for Page ${currentPageIndex + 1}."
                var parcelFileDescriptor: ParcelFileDescriptor? = null
                var pdDocument: PDDocument? = null

                try {
                    // Re-open file descriptor for PDFBox, as PdfRenderer might hold it
                    parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(currentPdfUri!!, "r")
                    if (parcelFileDescriptor != null) {
                        println("Extracting text from Page ${currentPageIndex + 1}")
                        extractedText = currentPage?.textContents?.first()?.text
                    } else {
                        extractedText = "Could not open file descriptor for text extraction."
                    }
                } catch (e: Exception) {
                    extractedText = "Error extracting text: ${e.message}"
                } finally {
                    // Close resources
                    try {
                        pdDocument?.close()
                        parcelFileDescriptor?.close()
                    } catch (e: IOException) {
                        println("Error closing PDFBox resources $e")
                    }
                }

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    println("Extracted Text: $extractedText")
                    binding.textPreview.text = currentPage?.textContents?.first()?.text
                    binding.textPreview.scrollTo(0, 0)
                    updateButtonStates() // Update button states after text is loaded
                }
            }

            // Update button states
            updateButtonStates()

        } catch (e: Exception) {
            println("Error rendering page: ${e.message}")

        }
    }
    /**
     * Updates the enabled/disabled state of the previous and next buttons
     * based on the current page index.
     */
    private fun updateButtonStates() {
        if (pdfRenderer != null) {
            prevButton.isEnabled = currentPageIndex > 0
            nextButton.isEnabled = currentPageIndex < pdfRenderer!!.pageCount - 1
        } else {
            prevButton.isEnabled = false
            nextButton.isEnabled = false
        }
    }
}