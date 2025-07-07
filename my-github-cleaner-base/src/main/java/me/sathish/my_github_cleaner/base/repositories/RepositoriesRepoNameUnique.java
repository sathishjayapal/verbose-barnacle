package me.sathish.my_github_cleaner.base.repositories;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Validate that the repoName value isn't taken yet.
 */
@Target({FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = RepositoriesRepoNameUnique.RepositoriesRepoNameUniqueValidator.class)
public @interface RepositoriesRepoNameUnique {

    String message() default "{exists.repositories.repoName}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class RepositoriesRepoNameUniqueValidator implements ConstraintValidator<RepositoriesRepoNameUnique, String> {

        private final RepositoriesService repositoriesService;
        private final HttpServletRequest request;

        public RepositoriesRepoNameUniqueValidator(
                final RepositoriesService repositoriesService, final HttpServletRequest request) {
            this.repositoriesService = repositoriesService;
            this.request = request;
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked")
            final Map<String, String> pathVariables =
                    ((Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null
                    && value.equalsIgnoreCase(
                            repositoriesService.get(Long.parseLong(currentId)).getRepoName())) {
                // value hasn't changed
                return true;
            }
            return !repositoriesService.repoNameExists(value);
        }
    }
}
