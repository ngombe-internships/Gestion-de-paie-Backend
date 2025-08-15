package com.hades.maalipo.controller;

import com.hades.maalipo.dto.reponse.ApiResponse;
import com.hades.maalipo.dto.conge.DocumentDto;
import com.hades.maalipo.model.User;
import com.hades.maalipo.service.EmployeService;
import com.hades.maalipo.service.conge.DocumentCongeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/conge/documents")
public class DocumentCongeController {

    private final DocumentCongeService documentService;
    private final EmployeService employeService;

    public DocumentCongeController(DocumentCongeService documentService,
                                   EmployeService employeService) {
        this.documentService = documentService;
        this.employeService = employeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("demandeId") Long demandeId,
            @RequestParam("typeDocument") DocumentCongeService.TypeDocument typeDocument) {

        try {
            User currentUser = employeService.getAuthenticatedUser();
            //String filePath = documentService.stockerDocument(file, demandeId, typeDocument, currentUser);
            DocumentDto document = documentService.uploadToCloudinary(file, demandeId, typeDocument, currentUser);

            return ResponseEntity.ok(new ApiResponse<>(
                    "Document téléversé avec succès",
                    document,
                    HttpStatus.OK
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST
            ));
        }
    }

    @GetMapping("/{demandeId}")
    public ResponseEntity<ApiResponse<List<DocumentDto>>> getDocumentsForDemande(
            @PathVariable Long demandeId) {

        try {
            List<DocumentDto> documents = documentService.getDocumentsForDemande(demandeId);

            return ResponseEntity.ok(new ApiResponse<>(
                    "Documents récupérés avec succès",
                    documents,
                    HttpStatus.OK
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                    e.getMessage(),
                    null,
                    HttpStatus.BAD_REQUEST
            ));
        }
    }
}