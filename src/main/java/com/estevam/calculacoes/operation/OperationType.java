package com.estevam.calculacoes.operation;

/**
 * Enum representing the type of stock operation.
 */
public enum OperationType {
    BUY("compra"),
    SELL("venda"),
    POSITION("posição");

    private final String portugueseLabel;

    OperationType(String portugueseLabel) {
        this.portugueseLabel = portugueseLabel;
    }

    public String getPortugueseLabel() {
        return portugueseLabel;
    }

    /**
     * Converts a Portuguese label to the corresponding enum value.
     */
    public static OperationType fromPortuguese(String label) {
        for (OperationType type : values()) {
            if (type.portugueseLabel.equalsIgnoreCase(label.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown operation type: " + label);
    }
}