class MethodRelation:
    def __init__(self, source_class: str, source_method: str, target_class: str, target_method: str):
        self.source_class = source_class
        self.source_method = source_method
        self.target_class = target_class
        self.target_method = target_method

    def __str__(self):
        return f"{self.source_class}.{self.source_method} -> {self.target_class}.{self.target_method}"
