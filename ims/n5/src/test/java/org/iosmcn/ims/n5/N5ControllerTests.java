package org.iosmcn.ims.n5;

import org.iosmcn.ims.n5.controller.NFController;
import org.iosmcn.ims.n5.service.NFService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import java.util.Map;

/**
 * N5ControllerTests.java
 * This class contains unit tests for the NFController.
 * It uses Spring Boot's testing framework to test the controller endpoints in isolation.
 */
@WebMvcTest(NFController.class) // Only loads NFController (not the full application context)
class N5ControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private NFService nfService; // Mock NFService to prevent real calls

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePDUAudioApiReachable() throws Exception {
        when(nfService.createPDUAudio(any(Map.class))).thenReturn("Mock PDU Audio Created");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/nf/createPDUAudio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePDUVideoApiReachable() throws Exception {
        when(nfService.createPDUVideo(any(Map.class))).thenReturn("Mock PDU Video Created");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/nf/createPDUVideo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeletePDUApiReachable() throws Exception {
        when(nfService.deletePDU(any(Map.class))).thenReturn("Mock PDU Deleted");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/nf/deletePDU")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());
    }
}
