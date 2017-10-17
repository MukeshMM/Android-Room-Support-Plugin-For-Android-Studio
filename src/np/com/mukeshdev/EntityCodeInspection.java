package np.com.mukeshdev;

import com.intellij.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Check for Primary Key and check if it is correct
 */
public class EntityCodeInspection extends BaseJavaLocalInspectionTool {


    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Room Entity Helper";
    }

    @Nls
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Android Room";
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(PsiClass aClass) {
                super.visitClass(aClass);

                try {
                    PsiModifierList psiClassModifierList = aClass.getModifierList();

                    if (psiClassModifierList == null) {
                        return;
                    }

                    PsiAnnotation psiClassAnnotation = psiClassModifierList.findAnnotation("android.arch.persistence.room.Entity");


                    if (psiClassAnnotation != null) {
                        runPrimaryKeyCodeInspection(aClass, psiClassAnnotation, holder);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };


    }

    private void runPrimaryKeyCodeInspection(PsiClass aClass, PsiAnnotation psiClassAnnotation, @NotNull ProblemsHolder holder) {
        PsiNameValuePair[] psiClassAnnotationParameterNameValuePairs =
                psiClassAnnotation.getParameterList().getAttributes();
        PsiField[] psiFields = aClass.getAllFields();

        PsiAnnotation psiFieldsAnnotation;
        PsiModifierList psiFieldsModifierList;

        for (PsiField psiField : psiFields) {
            psiFieldsModifierList = psiField.getModifierList();

            if (psiFieldsModifierList == null) {
                break;
            }
            psiFieldsAnnotation = psiFieldsModifierList.findAnnotation("android.arch.persistence.room.PrimaryKey");

            // We have found Primary Key
            // Field Already has Primary Key Annotation
            if (psiFieldsAnnotation != null) {
                return;
            }
        }

        String annotationParameterAttributeName;
        String annotationParameterAttributeValue;
        boolean annotationParameterHasPrimaryKey = false;
        StringBuilder availableColumnNames = new StringBuilder();
        String fieldName;

        for (PsiNameValuePair psiClassAnnotationParameterNameValuePair : psiClassAnnotationParameterNameValuePairs) {
            annotationParameterAttributeName = psiClassAnnotationParameterNameValuePair.getName();
            annotationParameterAttributeValue = psiClassAnnotationParameterNameValuePair.getLiteralValue();

            if (annotationParameterAttributeName != null && annotationParameterAttributeValue != null)
                if (annotationParameterAttributeName.equals("primaryKeys")) {
                    annotationParameterHasPrimaryKey = true;
                    for (PsiField psiField : psiFields) {
                        fieldName = psiField.getName();

                        if (fieldName != null) {
                            availableColumnNames.append(fieldName).append(",");
                            if (fieldName.
                                    equals(annotationParameterAttributeValue)) {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                }
        }

        PsiModifierList psiModifierList = aClass.getModifierList();
        if (annotationParameterHasPrimaryKey) {
            availableColumnNames.deleteCharAt(availableColumnNames.length()-1);
            holder.registerProblem(psiModifierList,
                    "Error: Referenced in the primary key does not exists in the Entity. " +
                            "Available column names:" + availableColumnNames
                    , ProblemHighlightType.GENERIC_ERROR);
        } else {
            holder.registerProblem(psiModifierList,
                    "Error: An entity must have at least 1 field annotated with @PrimaryKey"
                    , ProblemHighlightType.GENERIC_ERROR);
        }
    }
}