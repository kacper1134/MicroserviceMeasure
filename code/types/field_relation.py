class FieldRelation:
    def __init__(self, source_class: str, source_method: str, target_class: str, target_field: str):
        self.source_class = source_class
        self.source_method = source_method
        self.target_class = target_class
        self.target_field = target_field

    def __str__(self):
        return f"{self.source_class}.{self.source_method} -> {self.target_class}.{self.target_field}"
