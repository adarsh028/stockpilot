package com.stockpilot.product;

import com.stockpilot.product.dto.SkuImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skus/{skuId}/images")
@RequiredArgsConstructor
public class SkuImageController {

    private final SkuImageService skuImageService;

    @GetMapping
    public List<SkuImageResponse> list(@PathVariable UUID skuId) {
        return skuImageService.list(skuId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SkuImageResponse upload(@PathVariable UUID skuId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "primary", defaultValue = "false") boolean primary) {
        return skuImageService.upload(skuId, file, primary);
    }

    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public SkuImageResponse setPrimary(@PathVariable UUID skuId, @PathVariable UUID imageId) {
        return skuImageService.setPrimary(skuId, imageId);
    }

    @DeleteMapping("/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public void delete(@PathVariable UUID skuId, @PathVariable UUID imageId) {
        skuImageService.delete(skuId, imageId);
    }
}
