import neonAPI


def main():
    products = neonAPI.getTaxonomy("a", "b", "c", "d", "e", "f", "g", "h", 5)
    with open("output.txt", "w") as file:
        file.write(neonAPI.prettyPrint(neonAPI.getTaxonomy()))


if __name__ == '__main__':
    main()
