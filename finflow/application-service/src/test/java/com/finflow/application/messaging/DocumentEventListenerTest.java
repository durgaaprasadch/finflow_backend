package com.finflow.application.messaging;

import com.finflow.application.dto.DocumentMessage;
import com.finflow.application.entity.InboundEvent;
import com.finflow.application.exception.DocumentException;
import com.finflow.application.repository.InboundEventRepository;
import com.finflow.application.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DocumentEventListenerTest {

    @Mock
    private LoanApplicationService applicationService;

    @Mock
    private InboundEventRepository inboundEventRepository;

    @InjectMocks
    private DocumentEventListener listener;

    @Test
    void handleDocumentUploaded_ShouldProcessEvent_WhenNew() {
        DocumentMessage message = new DocumentMessage();
        message.setEventId("event-1");
        message.setApplicationId(1L);

        when(inboundEventRepository.existsById("event-1")).thenReturn(false);

        listener.handleDocumentUploaded(message);

        verify(applicationService, times(1)).syncStatusAfterDocumentUpload(1L);
        verify(inboundEventRepository, times(1)).save(any(InboundEvent.class));
    }

    @Test
    void handleDocumentUploaded_ShouldSkip_WhenDuplicate() {
        DocumentMessage message = new DocumentMessage();
        message.setEventId("event-1");

        when(inboundEventRepository.existsById("event-1")).thenReturn(true);

        listener.handleDocumentUploaded(message);

        verifyNoInteractions(applicationService);
        verify(inboundEventRepository, never()).save(any(InboundEvent.class));
    }

    @Test
    void handleDocumentUploaded_ShouldThrowDocumentException_WhenErrorOccurs() {
        DocumentMessage message = new DocumentMessage();
        message.setEventId("event-1");
        message.setApplicationId(1L);

        when(inboundEventRepository.existsById("event-1")).thenReturn(false);
        doThrow(new RuntimeException("DB Error")).when(applicationService).syncStatusAfterDocumentUpload(1L);

        assertThrows(DocumentException.class, () -> listener.handleDocumentUploaded(message));
    }
}
