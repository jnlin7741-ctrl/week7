package edu.macslab.board;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import edu.macslab.board.ApiModels.ErrorView;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorView> known(ApiException e) {
        return ResponseEntity.status(e.status).body(new ErrorView(e.code,e.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,
                       MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorView> invalid(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorView("INVALID_INPUT",
            "Check the fields and their lengths. Remove unknown fields."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorView> conflict(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(new ErrorView("CONFLICT","This account already exists or the data conflicts."));
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorView> changed(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(409).body(new ErrorView("CONFLICT","The post changed. Reload and try again."));
    }
}
