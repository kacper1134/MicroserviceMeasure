class MethodRelation:
    def __init__(self, source_class: str, source_method_signature: str, target_class: str, target_method_signature: str):
        self.source_class = source_class
        self.source_method_signature = source_method_signature
        self.target_class = target_class
        self.target_method_signature = target_method_signature

    def __str__(self):
        return f"{self.source_class}.{self.target_method_signature} -> {self.target_class}.{self.target_method_signature}"
