package ch.so.agi.umleditor.webservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import ch.so.agi.umleditor.UmlDiagramVendor;
import ch.so.agi.umleditor.UmlEditorUtility;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MainController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.ilidirs}")
    private String ilidirs;

    @GetMapping("/ping")
    public ResponseEntity<String> ping(@RequestHeader Map<String, String> headers, HttpServletRequest request) {
        headers.forEach((key, value) -> {
            log.info(String.format("Header '%s' = %s", key, value));
        });
        log.info("server name: " + request.getServerName());
        log.info("context path: " + request.getContextPath());
        log.info("ping"); 
        return new ResponseEntity<String>("iliprettyprint-web-service", HttpStatus.OK);
    }

    @PostMapping(value = "/api/prettyprint", consumes = {"multipart/form-data"}, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> prettyprint(@RequestPart(name = "file", required = true) MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }
        
        try {
            Path inDir = Files.createTempDirectory("pprint_input_");
            Path iliFile = inDir.resolve(file.getOriginalFilename());
            file.transferTo(iliFile);
            
            Path outDir = Files.createTempDirectory("pprint_output_");
            boolean ret = UmlEditorUtility.prettyPrint(iliFile, ilidirs, outDir);
                                   
            String iliPrettyPrinted = Files.readString(Paths.get(outDir.toString(), iliFile.getFileName().toString()));
           
            deleteTemporaryFiles(inDir, outDir);
            
            if (ret) {
                return ResponseEntity.ok(iliPrettyPrinted);
            } else {
                return ResponseEntity.internalServerError().body("error while converting ili file");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping(value = "/api/uml", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uml(@RequestPart(name = "file", required = true) MultipartFile file, @RequestPart(name = "vendor", required = false) String vendorParam) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }
        
        UmlDiagramVendor vendor = UmlDiagramVendor.PLANTUML;
        if (vendorParam != null) {
            vendor = UmlDiagramVendor.valueOf(vendorParam);
        }
                        
        try {
            Path inDir = Files.createTempDirectory("uml_input_");
            Path iliFile = inDir.resolve(file.getOriginalFilename());
            file.transferTo(iliFile);
            
            Path outDir = Files.createTempDirectory("uml_output_");            
            Path umlFile = UmlEditorUtility.createUmlDiagram(iliFile, ilidirs, outDir, vendor);
            if (umlFile == null) {
                return ResponseEntity.internalServerError().body("error while creating uml diagram");   
            }                

            if (vendor.equals(UmlDiagramVendor.MERMAID)) {
                String mermaidFileContent = Files.readString(umlFile);

                deleteTemporaryFiles(inDir, outDir);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_HTML);
//                htmlTemplate = """
//                        aaa `%s`
//                        asdf
//                        """;
                return new ResponseEntity<>(htmlTemplate.formatted(mermaidFileContent), headers, HttpStatus.OK);
            } else {
                byte[] imageBytes;
                try (InputStream in = Files.newInputStream(umlFile)) {
                    imageBytes = in.readAllBytes();
                }
                
                deleteTemporaryFiles(inDir, outDir);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void deleteTemporaryFiles(Path...dirs) throws IOException {
        for (Path dir : dirs) {
            FileSystemUtils.deleteRecursively(dir);            
        }
    }
    
    private String htmlTemplate = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <title>Mermaid Diagram</title>
  <script type="module">
    import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';

    let direction = 'TB'; // initial direction

    const baseCode = `
%s
    `;

    //mermaid.initialize({ startOnLoad: false, flowchart: { curve: "basis", nodeSpacing: 50, rankSpacing: 50 } });

    const renderDiagram = async () => {
      const { svg } = await mermaid.render('theDiagram', baseCode);
      const container = document.getElementById('diagramContainer');
      container.innerHTML = svg;

      // Enable pan/zoom
      if (window.svgPanZoomInstance) {
        window.svgPanZoomInstance.destroy();
      }
      const svgElement = container.querySelector('svg');
      window.svgPanZoomInstance = svgPanZoom(svgElement, {
        zoomEnabled: true,
        controlIconsEnabled: false,
        fit: true,
        center: true,
        minZoom: 0.2,
        maxZoom: 10,
        panEnabled: true
      });

      document.getElementById('downloadSvgBtn').onclick = () => {
        const blob = new Blob([svg], { type: 'image/svg+xml' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'diagram.svg';
        a.click();
        URL.revokeObjectURL(url);
      };

      document.getElementById('copyCodeBtn').onclick = async () => {
        const btnSpan = document.querySelector('#copyCodeBtn span');
        try {
          await navigator.clipboard.writeText(fullCode);
          btnSpan.textContent = 'Copied!';
          document.getElementById('copyCodeBtn').disabled = true;
          setTimeout(() => {
            btnSpan.textContent = 'Copy Mermaid Code';
            document.getElementById('copyCodeBtn').disabled = false;
          }, 4000);
        } catch (err) {
          console.error('Clipboard copy failed:', err);
        }
      };
    };

    window.addEventListener('DOMContentLoaded', async () => {
      await renderDiagram();

      document.getElementById('toggleDirectionBtn').addEventListener('click', async () => {
        direction = direction === 'TB' ? 'LR' : 'TB';
        await renderDiagram();
      });
    });
  </script>

  <script src="https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js"></script>

  <style>
    html, body {
      margin: 0;
      padding: 0;
      height: 100vh;
      display: flex;
      flex-direction: column;
      font-family: sans-serif;
    }

    .controls {
      padding: 1em;
      background: white;
      border-bottom: 1px solid #ccc;
      display: flex;
      gap: 1em;
      flex-wrap: wrap;
    }

    button {
      padding: 0.5em 1em;
      font-size: 1em;
      width: 240px;
      cursor: pointer;
      background: #ECECFF;
      border: 2px solid #9370DA;
      border-radius: 5px;
    }

    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    #diagramContainer {
      flex: 1;
      background: white;
      display: flex;
      justify-content: center;
      align-items: center;
      overflow: hidden;
    }

    svg {
      width: 100%%;
      height: 100%%;
      max-width: 100%% !important;
    }
  </style>
</head>
<body>
  <div class="controls">
    <button id="downloadSvgBtn"><span>Download SVG</span></button>
    <button id="copyCodeBtn"><span>Copy Mermaid Code</span></button>
  </div>
  <div id="diagramContainer"></div>
</body>
</html>
            """;
}
