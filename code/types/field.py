from code.types.access_modifier import AccessModifier


class Field:
    def __init__(self, name: str, fieldType: str, modifier: AccessModifier):
        self.name = name
        self.type = fieldType
        self.modifier = modifier

    def __str__(self):
        return f"Field: {self.name} ({self.type}) ({self.modifier})"
