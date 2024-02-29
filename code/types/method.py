from typing import List

from code.types.access_modifier import AccessModifier


class Method:
    def __init__(self, signature: str, name: str, parameters: List[str], modifier: AccessModifier,
                 return_type: str):
        self.signature = signature
        self.name = name
        self.parameters = parameters
        self.modifier = modifier
        self.return_type = return_type
        self.number_of_lines = 1

    def __str__(self):
        return f"Method Name: {self.name}\n" \
               f"Parameters: {self.parameters}\n" \
               f"Modifier: {self.modifier}\n" \
               f"Return Type: {self.return_type}\n"

    def __eq__(self, other):
        return (isinstance(other, Method) and
                self.signature == other.signature)

    def __hash__(self):
        return hash(self.signature)
