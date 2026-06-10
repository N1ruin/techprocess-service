package by.niruin.techprocess_service.unit;

import by.niruin.techprocess_service.domain.enums.TechnologicalProcessOrganizationType;
import by.niruin.techprocess_service.domain.enums.TechnologicalProcessWorkType;
import by.niruin.techprocess_service.service.TechnologicalProcessFullNumberBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TechnologicalProcessFullNumberBuilderTest {
    private final TechnologicalProcessFullNumberBuilder builder = new TechnologicalProcessFullNumberBuilder();

    @Test
    void buildFullNumber_shouldReturnCorrectFormat() {
        var organizationType = TechnologicalProcessOrganizationType.SINGLE;
        var workType = TechnologicalProcessWorkType.ASSEMBLY;
        var archiveNumber = "12345";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02188.12345");
    }

    @Test
    void buildFullNumber_withDifferentOrganizationType() {
        var organizationType = TechnologicalProcessOrganizationType.TYPICAL;
        var workType = TechnologicalProcessWorkType.ASSEMBLY;
        var archiveNumber = "54321";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02288.54321");
    }

    @Test
    void buildFullNumber_withDifferentWorkType() {
        var organizationType = TechnologicalProcessOrganizationType.SINGLE;
        var workType = TechnologicalProcessWorkType.GENERAL_PURPOSE;
        var archiveNumber = "99999";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02101.99999");
    }

    @Test
    void buildFullNumber_withEmptyArchiveNumber() {
        var organizationType = TechnologicalProcessOrganizationType.SINGLE;
        var workType = TechnologicalProcessWorkType.ASSEMBLY;
        var archiveNumber = "";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02188.");
    }

    @Test
    void buildFullNumber_withLongArchiveNumber() {
        var organizationType = TechnologicalProcessOrganizationType.GROUP;
        var workType = TechnologicalProcessWorkType.TESTS;
        var archiveNumber = "123456789012345";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02306.123456789012345");
    }

    @Test
    void buildFullNumber_withAllOrganizationTypes() {
        var workType = TechnologicalProcessWorkType.ASSEMBLY;
        var archiveNumber = "11111";

        for (var organizationType : TechnologicalProcessOrganizationType.values()) {
            var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

            assertThat(result).startsWith("100316761292.");
            assertThat(result).endsWith(archiveNumber);
            assertThat(result).containsPattern("\\d{12}\\.\\d{5}\\.\\d{5}");
        }
    }

    @Test
    void buildFullNumber_withAllWorkTypes() {
        var organizationType = TechnologicalProcessOrganizationType.SINGLE;
        var archiveNumber = "22222";

        for (var workType : TechnologicalProcessWorkType.values()) {
            var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

            assertThat(result).startsWith("100316761292.");
            assertThat(result).endsWith(archiveNumber);
            assertThat(result).containsPattern("\\d{12}\\.\\d{5}\\.\\d{5}");
        }
    }

    @Test
    void buildFullNumber_shouldHaveCorrectLength() {
        var organizationType = TechnologicalProcessOrganizationType.SINGLE;
        var workType = TechnologicalProcessWorkType.ASSEMBLY;
        var archiveNumber = "12345";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).hasSize(12 + 1 + 5 + 1 + 5);
        assertThat(result).matches("\\d{12}\\.\\d{5}\\.\\d{5}");
    }

    @Test
    void buildFullNumber_withGroupOrganizationAndTestsWorkType() {
        var organizationType = TechnologicalProcessOrganizationType.GROUP;
        var workType = TechnologicalProcessWorkType.TESTS;
        var archiveNumber = "77777";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02306.77777");
    }

    @Test
    void buildFullNumber_withTypicalOrganizationAndGeneralPurposeWorkType() {
        var organizationType = TechnologicalProcessOrganizationType.TYPICAL;
        var workType = TechnologicalProcessWorkType.GENERAL_PURPOSE;
        var archiveNumber = "88888";

        var result = builder.buildFullNumber(organizationType, workType, archiveNumber);

        assertThat(result).isEqualTo("100316761292.02201.88888");
    }
}